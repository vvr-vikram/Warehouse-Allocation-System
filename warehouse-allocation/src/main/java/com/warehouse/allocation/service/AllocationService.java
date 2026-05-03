package com.warehouse.allocation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.warehouse.allocation.dto.AllocationRequest;
import com.warehouse.allocation.dto.AllocationResponse;
import com.warehouse.allocation.dto.PaginatedResponse;
import com.warehouse.allocation.dto.SearchAllocationsRequest;
import com.warehouse.allocation.entity.Allocation;
import com.warehouse.allocation.entity.AuditLog;
import com.warehouse.allocation.entity.Product;
import com.warehouse.allocation.entity.Warehouse;
import com.warehouse.allocation.entity.WarehouseInventory;
import com.warehouse.allocation.exception.AllocationException.AllocationNotFoundException;
import com.warehouse.allocation.exception.AllocationException.ConcurrentModificationException;
import com.warehouse.allocation.exception.AllocationException.InsufficientStockException;
import com.warehouse.allocation.exception.AllocationException.InvalidStateException;
import com.warehouse.allocation.exception.AllocationException.InventoryNotFoundException;
import com.warehouse.allocation.exception.AllocationException.ProductNotFoundException;
import com.warehouse.allocation.exception.AllocationException.WarehouseNotFoundException;
import com.warehouse.allocation.repository.AllocationRepository;
import com.warehouse.allocation.repository.ProductRepository;
import com.warehouse.allocation.repository.WarehouseInventoryRepository;
import com.warehouse.allocation.repository.WarehouseRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class AllocationService {
    
    private final AllocationRepository allocationRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final AuditService auditService;
    
    public AllocationService(
            AllocationRepository allocationRepository,
            WarehouseInventoryRepository inventoryRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            AuditService auditService) {
        this.allocationRepository = allocationRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.auditService = auditService;
    }
    
    
      //Allocate stock from a specific warehouse
     
    public AllocationResponse allocateFromWarehouse(AllocationRequest request) {
        log.info("Allocating {} units of product {} from warehouse {}", 
            request.getQuantity(), request.getProductId(), request.getWarehouseId());
        
        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found with ID: " + request.getProductId()));
        
        // Validate warehouse exists
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new WarehouseNotFoundException(
                "Warehouse not found with ID: " + request.getWarehouseId()));
        
        // Get inventory for the warehouse-product combination
        WarehouseInventory inventory = inventoryRepository
            .findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
            .orElseThrow(() -> new InventoryNotFoundException(
                "Inventory not found for warehouse " + warehouse.getId() + 
                " and product " + product.getId()));
        
        // Check if sufficient stock available
        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            log.warn("Insufficient stock: Available={}, Requested={}", 
                inventory.getAvailableQuantity(), request.getQuantity());
            throw new InsufficientStockException(
                String.format("Insufficient stock. Available: %d, Requested: %d",
                    inventory.getAvailableQuantity(), request.getQuantity()));
        }
        
        // Perform allocation with optimistic locking
        try {
            inventory.allocate(request.getQuantity());
            inventoryRepository.save(inventory);
            
            // Create allocation record
            Allocation allocation = Allocation.builder()
                .product(product)
                .warehouse(warehouse)
                .quantity(request.getQuantity())
                .status(Allocation.AllocationStatus.CONFIRMED)
                .allocatedAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .referenceId(generateReferenceId())
                .notes(request.getNotes())
                .build();
            
            Allocation savedAllocation = allocationRepository.save(allocation);
            log.info("Allocation successful with reference ID: {}", savedAllocation.getReferenceId());
            
            // Audit logging
            auditService.logAction(
                "Allocation",
                savedAllocation.getId(),
                AuditLog.AuditAction.ALLOCATE,
                null,
                savedAllocation,
                "Allocated " + request.getQuantity() + " units of " + product.getName()
            );
            
            return mapToAllocationResponse(savedAllocation);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            log.error("Concurrent modification detected during allocation", ex);
            throw new ConcurrentModificationException(
                "Concurrent modification detected. Please retry the allocation.");
        }
    }
    
    
     //Auto-select warehouse based on available stock
     
    public AllocationResponse allocateFromBestWarehouse(AllocationRequest request) {
        log.info("Auto-selecting warehouse for product {} with quantity {}", 
            request.getProductId(), request.getQuantity());
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found with ID: " + request.getProductId()));
        
        // Find warehouses with sufficient stock, sorted by available quantity (ascending)
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.asc("availableQuantity")));
        List<WarehouseInventory> availableInventories = inventoryRepository
            .findWarehousesWithSufficientStock(product.getId(), request.getQuantity(), pageable);
        
        if (availableInventories.isEmpty()) {
            throw new InsufficientStockException(
                "No warehouse has sufficient stock for product: " + product.getName());
        }
        
        WarehouseInventory selectedInventory = availableInventories.get(0);
        AllocationRequest modifiedRequest = AllocationRequest.builder()
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .warehouseId(selectedInventory.getWarehouse().getId())
            .notes(request.getNotes())
            .build();
        
        return allocateFromWarehouse(modifiedRequest);
    }
    
   
     //Confirm a pending allocation
     
    public AllocationResponse confirmAllocation(Long allocationId) {
        log.info("Confirming allocation with ID: {}", allocationId);
        
        Allocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new AllocationNotFoundException(
                "Allocation not found with ID: " + allocationId));
        
        if (!allocation.getStatus().equals(Allocation.AllocationStatus.PENDING)) {
            throw new InvalidStateException(
                "Allocation is not in PENDING state. Current status: " + allocation.getStatus());
        }
        
        allocation.setStatus(Allocation.AllocationStatus.CONFIRMED);
        allocation.setConfirmedAt(LocalDateTime.now());
        Allocation updated = allocationRepository.save(allocation);
        
        auditService.logAction(
            "Allocation",
            updated.getId(),
            AuditLog.AuditAction.CONFIRM,
            allocation,
            updated,
            "Allocation confirmed"
        );
        
        return mapToAllocationResponse(updated);
    }
    
    
     //Rollback a pending allocation
     
    public AllocationResponse rollbackAllocation(Long allocationId) {
        log.info("Rolling back allocation with ID: {}", allocationId);
        
        Allocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new AllocationNotFoundException(
                "Allocation not found with ID: " + allocationId));
        
        if (!allocation.getStatus().equals(Allocation.AllocationStatus.PENDING)) {
            throw new InvalidStateException(
                "Cannot rollback allocation. Current status: " + allocation.getStatus());
        }
        
        // Return stock to inventory
        WarehouseInventory inventory = inventoryRepository
            .findByWarehouseIdAndProductId(allocation.getWarehouse().getId(), allocation.getProduct().getId())
            .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
        
        inventory.deallocate(allocation.getQuantity());
        inventoryRepository.save(inventory);
        
        allocation.setStatus(Allocation.AllocationStatus.ROLLED_BACK);
        Allocation updated = allocationRepository.save(allocation);
        
        auditService.logAction(
            "Allocation",
            updated.getId(),
            AuditLog.AuditAction.ROLLBACK,
            allocation,
            updated,
            "Allocation rolled back"
        );
        
        return mapToAllocationResponse(updated);
    }
    
    
     // Get allocation by ID
     
    @Transactional(readOnly = true)
    public AllocationResponse getAllocationById(Long allocationId) {
        Allocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new AllocationNotFoundException(
                "Allocation not found with ID: " + allocationId));
        return mapToAllocationResponse(allocation);
    }
    
    
     //Search allocations with filters
     
    @Transactional(readOnly = true)
    public PaginatedResponse<AllocationResponse> searchAllocations(SearchAllocationsRequest request) {
        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.Direction.fromString(request.getDirection()),
            request.getSortBy()
        );
        
        Allocation.AllocationStatus status = request.getStatus() != null ?
            Allocation.AllocationStatus.valueOf(request.getStatus()) : null;
        
        Page<Allocation> page = allocationRepository.searchAllocations(
            request.getProductId(),
            request.getWarehouseId(),
            status,
            request.getStartDate(),
            request.getEndDate(),
            pageable
        );
        
        return PaginatedResponse.<AllocationResponse>builder()
            .content(page.getContent().stream()
                .map(this::mapToAllocationResponse)
                .collect(Collectors.toList()))
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .isLast(page.isLast())
            .build();
    }
    
    
     //Get all allocations for a product
     
    @Transactional(readOnly = true)
    public List<AllocationResponse> getAllocationsForProduct(Long productId) {
        return allocationRepository.findByProductIdOrderByAllocatedAtDesc(productId).stream()
            .map(this::mapToAllocationResponse)
            .collect(Collectors.toList());
    }
   
    
     // Get all allocations for a warehouse
     
    @Transactional(readOnly = true)
    public List<AllocationResponse> getAllocationsForWarehouse(Long warehouseId) {
        return allocationRepository.findByWarehouseIdOrderByAllocatedAtDesc(warehouseId).stream()
            .map(this::mapToAllocationResponse)
            .collect(Collectors.toList());
    }
    
    
     // Generate unique reference ID
     
    private String generateReferenceId() {
        return "ALLOC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    
     //Map Allocation entity to DTO
     
    private AllocationResponse mapToAllocationResponse(Allocation allocation) {
        return AllocationResponse.builder()
            .id(allocation.getId())
            .productId(allocation.getProduct().getId())
            .productName(allocation.getProduct().getName())
            .warehouseId(allocation.getWarehouse().getId())
            .warehouseName(allocation.getWarehouse().getName())
            .quantity(allocation.getQuantity())
            .status(allocation.getStatus().toString())
            .referenceId(allocation.getReferenceId())
            .allocatedAt(allocation.getAllocatedAt())
            .confirmedAt(allocation.getConfirmedAt())
            .completedAt(allocation.getCompletedAt())
            .notes(allocation.getNotes())
            .createdAt(allocation.getCreatedAt())
            .build();
    }
}