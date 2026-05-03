package com.warehouse.allocation.service;

import com.warehouse.allocation.dto.StockTransferRequest;
import com.warehouse.allocation.dto.StockTransferResponse;
import com.warehouse.allocation.entity.*;
import com.warehouse.allocation.exception.*;
import com.warehouse.allocation.exception.AllocationException.InsufficientStockException;
import com.warehouse.allocation.exception.AllocationException.InvalidStateException;
import com.warehouse.allocation.exception.AllocationException.InventoryNotFoundException;
import com.warehouse.allocation.exception.AllocationException.ProductNotFoundException;
import com.warehouse.allocation.exception.AllocationException.WarehouseNotFoundException;
import com.warehouse.allocation.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class StockTransferService {
    
    private final StockTransferRepository transferRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;
    
    public StockTransferService(
            StockTransferRepository transferRepository,
            WarehouseInventoryRepository inventoryRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            AuditService auditService) {
        this.transferRepository = transferRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }
    
    
     // Initiate a stock transfer between warehouses
    
    public StockTransferResponse initiateTransfer(StockTransferRequest request) {
        log.info("Initiating stock transfer from warehouse {} to warehouse {} for product {} with quantity {}",
            request.getSourceWarehouseId(), request.getTargetWarehouseId(),
            request.getProductId(), request.getQuantity());
        
        // Validate warehouses
        Warehouse sourceWarehouse = warehouseRepository.findById(request.getSourceWarehouseId())
            .orElseThrow(() -> new WarehouseNotFoundException(
                "Source warehouse not found with ID: " + request.getSourceWarehouseId()));
        
        Warehouse targetWarehouse = warehouseRepository.findById(request.getTargetWarehouseId())
            .orElseThrow(() -> new WarehouseNotFoundException(
                "Target warehouse not found with ID: " + request.getTargetWarehouseId()));
        
        // Validate product
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found with ID: " + request.getProductId()));
        
        // Cannot transfer to same warehouse
        if (request.getSourceWarehouseId().equals(request.getTargetWarehouseId())) {
            throw new IllegalArgumentException("Source and target warehouses cannot be the same");
        }
        
        // Validate quantity
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }
        
        // Get source inventory
        WarehouseInventory sourceInventory = inventoryRepository
            .findByWarehouseIdAndProductId(request.getSourceWarehouseId(), request.getProductId())
            .orElseThrow(() -> new InventoryNotFoundException(
                "Source inventory not found"));
        
        // Check sufficient stock
        if (sourceInventory.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                String.format("Insufficient stock in source warehouse. Available: %d, Required: %d",
                    sourceInventory.getAvailableQuantity(), request.getQuantity()));
        }
        
        // Get or create target inventory
        WarehouseInventory targetInventory = inventoryRepository
            .findByWarehouseIdAndProductId(request.getTargetWarehouseId(), request.getProductId())
            .orElseGet(() -> {
                WarehouseInventory newInventory = WarehouseInventory.builder()
                    .warehouse(targetWarehouse)
                    .product(product)
                    .availableQuantity(0L)
                    .version(0L)
                    .build();
                return inventoryRepository.save(newInventory);
            });
        
        // Create transfer record
        StockTransfer transfer = StockTransfer.builder()
            .sourceWarehouse(sourceWarehouse)
            .targetWarehouse(targetWarehouse)
            .product(product)
            .quantity(request.getQuantity())
            .status(StockTransfer.TransferStatus.PENDING)
            .transferDate(LocalDateTime.now())
            .referenceId(generateTransferReferenceId())
            .notes(request.getNotes())
            .build();
        
        StockTransfer savedTransfer = transferRepository.save(transfer);
        
        auditService.logAction(
            "StockTransfer",
            savedTransfer.getId(),
            AuditLog.AuditAction.TRANSFER,
            null,
            savedTransfer,
            "Stock transfer initiated"
        );
        
        return mapToStockTransferResponse(savedTransfer);
    }
    
    
     // Execute a pending transfer
     
    public StockTransferResponse executeTransfer(Long transferId) {
        log.info("Executing transfer with ID: {}", transferId);
        
        StockTransfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new AllocationException(
                "Transfer not found with ID: " + transferId, "TRANSFER_NOT_FOUND"));
        
        if (!transfer.getStatus().equals(StockTransfer.TransferStatus.PENDING)) {
            throw new InvalidStateException(
                "Transfer is not in PENDING state. Current status: " + transfer.getStatus());
        }
        
        // Get inventories
        WarehouseInventory sourceInventory = inventoryRepository
            .findByWarehouseIdAndProductId(transfer.getSourceWarehouse().getId(), transfer.getProduct().getId())
            .orElseThrow(() -> new InventoryNotFoundException("Source inventory not found"));
        
        WarehouseInventory targetInventory = inventoryRepository
            .findByWarehouseIdAndProductId(transfer.getTargetWarehouse().getId(), transfer.getProduct().getId())
            .orElseThrow(() -> new InventoryNotFoundException("Target inventory not found"));
        
        try {
            // Transfer stock
            sourceInventory.transferStock(transfer.getQuantity());
            targetInventory.receiveStock(transfer.getQuantity());
            
            inventoryRepository.save(sourceInventory);
            inventoryRepository.save(targetInventory);
            
            // Update transfer status
            transfer.setStatus(StockTransfer.TransferStatus.COMPLETED);
            transfer.setCompletedDate(LocalDateTime.now());
            StockTransfer updated = transferRepository.save(transfer);
            
            auditService.logAction(
                "StockTransfer",
                updated.getId(),
                AuditLog.AuditAction.TRANSFER,
                transfer,
                updated,
                "Stock transfer completed"
            );
            
            return mapToStockTransferResponse(updated);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            log.error("Concurrent modification detected during transfer execution", ex);
            throw new ConcurrentModificationException(
                "Concurrent modification detected. Please retry the transfer.");
        }
    }
    
    
     //Cancel a pending transfer
     
    public void cancelTransfer(Long transferId) {
        log.info("Cancelling transfer with ID: {}", transferId);
        
        StockTransfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new AllocationException(
                "Transfer not found with ID: " + transferId, "TRANSFER_NOT_FOUND"));
        
        if (!transfer.getStatus().equals(StockTransfer.TransferStatus.PENDING)) {
            throw new InvalidStateException(
                "Cannot cancel transfer. Current status: " + transfer.getStatus());
        }
        
        transfer.setStatus(StockTransfer.TransferStatus.CANCELLED);
        StockTransfer updated = transferRepository.save(transfer);
        
        auditService.logAction(
            "StockTransfer",
            updated.getId(),
            AuditLog.AuditAction.UPDATE,
            transfer,
            updated,
            "Stock transfer cancelled"
        );
    }
    
    
     // Get transfer by ID
     
    @Transactional(readOnly = true)
    public StockTransferResponse getTransferById(Long transferId) {
        StockTransfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new AllocationException(
                "Transfer not found with ID: " + transferId, "TRANSFER_NOT_FOUND"));
        return mapToStockTransferResponse(transfer);
    }
    
    
     // Search transfers
     
    @Transactional(readOnly = true)
    public Page<StockTransferResponse> searchTransfers(
            Long sourceWarehouseId, Long targetWarehouseId, Long productId,
            String status, int page, int size, String sortBy, String direction) {
        
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.Direction.fromString(direction),
            sortBy
        );
        
        StockTransfer.TransferStatus transferStatus = status != null ?
            StockTransfer.TransferStatus.valueOf(status) : null;
        
        Page<StockTransfer> transfers = transferRepository.searchTransfers(
            sourceWarehouseId,
            targetWarehouseId,
            productId,
            transferStatus,
            pageable
        );
        
        return transfers.map(this::mapToStockTransferResponse);
    }
    
    
     // Generate unique transfer reference ID
     
    private String generateTransferReferenceId() {
        return "TRANS-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    
     //Map StockTransfer entity to DTO
     
    private StockTransferResponse mapToStockTransferResponse(StockTransfer transfer) {
        return StockTransferResponse.builder()
            .id(transfer.getId())
            .sourceWarehouseId(transfer.getSourceWarehouse().getId())
            .sourceWarehouseName(transfer.getSourceWarehouse().getName())
            .targetWarehouseId(transfer.getTargetWarehouse().getId())
            .targetWarehouseName(transfer.getTargetWarehouse().getName())
            .productId(transfer.getProduct().getId())
            .productName(transfer.getProduct().getName())
            .quantity(transfer.getQuantity())
            .status(transfer.getStatus().toString())
            .referenceId(transfer.getReferenceId())
            .transferDate(transfer.getTransferDate())
            .completedDate(transfer.getCompletedDate())
            .notes(transfer.getNotes())
            .createdAt(transfer.getCreatedAt())
            .build();
    }
}