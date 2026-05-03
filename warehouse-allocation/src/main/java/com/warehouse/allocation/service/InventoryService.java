package com.warehouse.allocation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.warehouse.allocation.dto.InventoryResponse;
import com.warehouse.allocation.entity.AuditLog;
import com.warehouse.allocation.entity.Product;
import com.warehouse.allocation.entity.Warehouse;
import com.warehouse.allocation.entity.WarehouseInventory;
import com.warehouse.allocation.exception.AllocationException.InventoryNotFoundException;
import com.warehouse.allocation.exception.AllocationException.ProductNotFoundException;
import com.warehouse.allocation.exception.AllocationException.WarehouseNotFoundException;
import com.warehouse.allocation.repository.ProductRepository;
import com.warehouse.allocation.repository.WarehouseInventoryRepository;
import com.warehouse.allocation.repository.WarehouseRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class InventoryService {
    
    private final WarehouseInventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;
    
    public InventoryService(
            WarehouseInventoryRepository inventoryRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            AuditService auditService) {
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }
    
    
     // Get inventory for a specific warehouse-product combination
     
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long warehouseId, Long productId) {
        WarehouseInventory inventory = inventoryRepository
            .findByWarehouseIdAndProductId(warehouseId, productId)
            .orElseThrow(() -> new InventoryNotFoundException(
                "Inventory not found for warehouse " + warehouseId + " and product " + productId));
        
        return mapToInventoryResponse(inventory);
    }
    
    
    // Get all inventory for a warehouse
     
    @Transactional(readOnly = true)
    public List<InventoryResponse> getWarehouseInventory(Long warehouseId) {
        // Validate warehouse exists
        warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new WarehouseNotFoundException(
                "Warehouse not found with ID: " + warehouseId));
        
        return inventoryRepository.findByWarehouseId(warehouseId).stream()
            .map(this::mapToInventoryResponse)
            .collect(Collectors.toList());
    }
    
    
     // Get all inventory for a product across all warehouses
     
    @Transactional(readOnly = true)
    public List<InventoryResponse> getProductInventory(Long productId) {
        // Validate product exists
        productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found with ID: " + productId));
        
        return inventoryRepository.findByProductId(productId).stream()
            .map(this::mapToInventoryResponse)
            .collect(Collectors.toList());
    }
    
    
     // Get total available stock for a product
     
    @Transactional(readOnly = true)
    public Long getTotalAvailableStock(Long productId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found with ID: " + productId));
        
        Long total = inventoryRepository.getTotalAvailableStock(productId);
        return total != null ? total : 0L;
    }
    
    
     //Initialize inventory for a warehouse-product combination
     
    public InventoryResponse initializeInventory(Long warehouseId, Long productId, Long quantity) {
        log.info("Initializing inventory for warehouse {} and product {} with quantity {}",
            warehouseId, productId, quantity);
        
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new WarehouseNotFoundException(
                "Warehouse not found with ID: " + warehouseId));
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found with ID: " + productId));
        
        // Check if inventory already exists
        if (inventoryRepository.findByWarehouseIdAndProductId(warehouseId, productId).isPresent()) {
            throw new IllegalArgumentException(
                "Inventory already exists for warehouse " + warehouseId + " and product " + productId);
        }
        
        WarehouseInventory inventory = WarehouseInventory.builder()
            .warehouse(warehouse)
            .product(product)
            .availableQuantity(quantity)
            .version(0L)
            .build();
        
        WarehouseInventory saved = inventoryRepository.save(inventory);
        
        auditService.logAction(
            "WarehouseInventory",
            saved.getId(),
            AuditLog.AuditAction.CREATE,
            null,
            saved,
            "Inventory initialized with quantity: " + quantity
        );
        
        return mapToInventoryResponse(saved);
    }
    
    
     // Map WarehouseInventory entity to DTO
     
    private InventoryResponse mapToInventoryResponse(WarehouseInventory inventory) {
        return InventoryResponse.builder()
            .id(inventory.getId())
            .warehouseId(inventory.getWarehouse().getId())
            .warehouseName(inventory.getWarehouse().getName())
            .productId(inventory.getProduct().getId())
            .productName(inventory.getProduct().getName())
            .availableQuantity(inventory.getAvailableQuantity())
            .version(inventory.getVersion())
            .createdAt(inventory.getCreatedAt())
            .updatedAt(inventory.getUpdatedAt())
            .build();
    }
}