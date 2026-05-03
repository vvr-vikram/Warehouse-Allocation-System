package com.warehouse.allocation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.warehouse.allocation.dto.CreateWarehouseRequest;
import com.warehouse.allocation.dto.WarehouseResponse;
import com.warehouse.allocation.entity.AuditLog;
import com.warehouse.allocation.entity.Warehouse;
import com.warehouse.allocation.exception.AllocationException.WarehouseNotFoundException;
import com.warehouse.allocation.repository.WarehouseRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class WarehouseService {
    
    private final WarehouseRepository warehouseRepository;
    private final AuditService auditService;
    
    public WarehouseService(WarehouseRepository warehouseRepository, AuditService auditService) {
        this.warehouseRepository = warehouseRepository;
        this.auditService = auditService;
    }
    
    
     // Create a new warehouse
     
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        log.info("Creating warehouse: {}", request.getName());
        
        // Check if warehouse with same name already exists
        warehouseRepository.findByNameAndIsDeletedFalse(request.getName())
            .ifPresent(w -> {
                throw new IllegalArgumentException("Warehouse with name '" + request.getName() + "' already exists");
            });
        
        Warehouse warehouse = Warehouse.builder()
            .name(request.getName())
            .location(request.getLocation())
            .capacity(request.getCapacity())
            .status(Warehouse.WarehouseStatus.ACTIVE)
            .isDeleted(false)
            .build();
        
        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created with ID: {}", saved.getId());
        
        auditService.logAction(
            "Warehouse",
            saved.getId(),
            AuditLog.AuditAction.CREATE,
            null,
            saved,
            "Warehouse created"
        );
        
        return mapToWarehouseResponse(saved);
    }
    
    
     //Get warehouse by ID
     
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
            .filter(w -> !w.getIsDeleted())
            .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found with ID: " + id));
        return mapToWarehouseResponse(warehouse);
    }
   
    
     //Get all active warehouses
     
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllActiveWarehouses() {
        return warehouseRepository.findByStatusAndIsDeletedFalse(Warehouse.WarehouseStatus.ACTIVE)
            .stream()
            .map(this::mapToWarehouseResponse)
            .collect(Collectors.toList());
    }
    
    
     //Get all warehouses with pagination
     
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAllWarehouses(int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.Direction.fromString(direction),
            sortBy
        );
        
        return warehouseRepository.findByIsDeletedFalse(pageable)
            .map(this::mapToWarehouseResponse);
    }
    
    
     //Update warehouse details
     
    public WarehouseResponse updateWarehouse(Long id, CreateWarehouseRequest request) {
        log.info("Updating warehouse with ID: {}", id);
        
        Warehouse warehouse = warehouseRepository.findById(id)
            .filter(w -> !w.getIsDeleted())
            .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found with ID: " + id));
        
        // Check if new name conflicts with another warehouse
        if (!warehouse.getName().equals(request.getName())) {
            warehouseRepository.findByNameAndIsDeletedFalse(request.getName())
                .ifPresent(w -> {
                    throw new IllegalArgumentException("Warehouse with name '" + request.getName() + "' already exists");
                });
        }
        
        Warehouse originalCopy = Warehouse.builder()
            .id(warehouse.getId())
            .name(warehouse.getName())
            .location(warehouse.getLocation())
            .capacity(warehouse.getCapacity())
            .status(warehouse.getStatus())
            .build();
        
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setCapacity(request.getCapacity());
        
        Warehouse updated = warehouseRepository.save(warehouse);
        log.info("Warehouse updated successfully with ID: {}", updated.getId());
        
        auditService.logAction(
            "Warehouse",
            updated.getId(),
            AuditLog.AuditAction.UPDATE,
            originalCopy,
            updated,
            "Warehouse updated"
        );
        
        return mapToWarehouseResponse(updated);
    }
   
    
     // Activate a warehouse
     
    public WarehouseResponse activateWarehouse(Long id) {
        log.info("Activating warehouse with ID: {}", id);
        
        Warehouse warehouse = warehouseRepository.findById(id)
            .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found with ID: " + id));
        
        if (warehouse.getStatus().equals(Warehouse.WarehouseStatus.ACTIVE)) {
            log.warn("Warehouse {} is already active", id);
            return mapToWarehouseResponse(warehouse);
        }
        
        warehouse.setStatus(Warehouse.WarehouseStatus.ACTIVE);
        Warehouse updated = warehouseRepository.save(warehouse);
        
        auditService.logAction(
            "Warehouse",
            updated.getId(),
            AuditLog.AuditAction.UPDATE,
            warehouse,
            updated,
            "Warehouse activated"
        );
        
        return mapToWarehouseResponse(updated);
    }
    
    
     // Deactivate a warehouse
     
    public WarehouseResponse deactivateWarehouse(Long id) {
        log.info("Deactivating warehouse with ID: {}", id);
        
        Warehouse warehouse = warehouseRepository.findById(id)
            .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found with ID: " + id));
        
        if (warehouse.getStatus().equals(Warehouse.WarehouseStatus.INACTIVE)) {
            log.warn("Warehouse {} is already inactive", id);
            return mapToWarehouseResponse(warehouse);
        }
        
        warehouse.setStatus(Warehouse.WarehouseStatus.INACTIVE);
        Warehouse updated = warehouseRepository.save(warehouse);
        
        auditService.logAction(
            "Warehouse",
            updated.getId(),
            AuditLog.AuditAction.UPDATE,
            warehouse,
            updated,
            "Warehouse deactivated"
        );
        
        return mapToWarehouseResponse(updated);
    }
    
    
     // Soft delete a warehouse
     
    public void softDeleteWarehouse(Long id) {
        log.info("Soft deleting warehouse with ID: {}", id);
        
        Warehouse warehouse = warehouseRepository.findById(id)
            .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found with ID: " + id));
        
        warehouse.setIsDeleted(true);
        warehouse.setStatus(Warehouse.WarehouseStatus.INACTIVE);
        warehouseRepository.save(warehouse);
        
        auditService.logAction(
            "Warehouse",
            warehouse.getId(),
            AuditLog.AuditAction.DELETE,
            warehouse,
            null,
            "Warehouse soft deleted"
        );
        
        log.info("Warehouse {} soft deleted successfully", id);
    }
    
    
     // Map Warehouse entity to DTO
     
    private WarehouseResponse mapToWarehouseResponse(Warehouse warehouse) {
        return WarehouseResponse.builder()
            .id(warehouse.getId())
            .name(warehouse.getName())
            .location(warehouse.getLocation())
            .capacity(warehouse.getCapacity())
            .status(warehouse.getStatus().toString())
            .createdAt(warehouse.getCreatedAt())
            .updatedAt(warehouse.getUpdatedAt())
            .build();
    }
}