package com.warehouse.allocation.service;

import com.warehouse.allocation.dto.AllocationRequest;
import com.warehouse.allocation.dto.AllocationResponse;
import com.warehouse.allocation.entity.*;
import com.warehouse.allocation.exception.*;
import com.warehouse.allocation.exception.AllocationException.AllocationNotFoundException;
import com.warehouse.allocation.exception.AllocationException.InsufficientStockException;
import com.warehouse.allocation.exception.AllocationException.InvalidStateException;
import com.warehouse.allocation.exception.AllocationException.ProductNotFoundException;
import com.warehouse.allocation.exception.AllocationException.WarehouseNotFoundException;
import com.warehouse.allocation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AllocationService Tests")
class AllocationServiceTest {
    
    @Mock
    private AllocationRepository allocationRepository;
    
    @Mock
    private WarehouseInventoryRepository inventoryRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private WarehouseRepository warehouseRepository;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private AllocationService allocationService;
    
    private Product product;
    private Warehouse warehouse;
    private WarehouseInventory inventory;
    private Allocation allocation;
    
    @BeforeEach
    void setUp() {
        product = Product.builder()
            .id(1L)
            .name("Laptop")
            .sku("LAPTOP-001")
            .totalStock(500L)
            .build();
        
        warehouse = Warehouse.builder()
            .id(1L)
            .name("Main Warehouse")
            .location("New York")
            .capacity(100000L)
            .status(Warehouse.WarehouseStatus.ACTIVE)
            .isDeleted(false)
            .build();
        
        inventory = WarehouseInventory.builder()
            .id(1L)
            .warehouse(warehouse)
            .product(product)
            .availableQuantity(150L)
            .version(0L)
            .build();
        
        allocation = Allocation.builder()
            .id(1L)
            .product(product)
            .warehouse(warehouse)
            .quantity(50L)
            .status(Allocation.AllocationStatus.CONFIRMED)
            .referenceId("ALLOC-123456")
            .build();
    }
    
    @Test
    @DisplayName("Should allocate stock successfully when sufficient stock available")
    void testAllocateFromWarehouse_Success() {
        AllocationRequest request = AllocationRequest.builder()
            .productId(1L)
            .warehouseId(1L)
            .quantity(50L)
            .notes("Test allocation")
            .build();
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L)).thenReturn(Optional.of(inventory));
        when(allocationRepository.save(any(Allocation.class))).thenReturn(allocation);
        
        AllocationResponse response = allocationService.allocateFromWarehouse(request);
        
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(50L, response.getQuantity());
        assertEquals("CONFIRMED", response.getStatus());
        verify(allocationRepository).save(any(Allocation.class));
        verify(auditService).logAction(anyString(), anyLong(), any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("Should throw InsufficientStockException when stock not available")
    void testAllocateFromWarehouse_InsufficientStock() {
        inventory.setAvailableQuantity(10L);
        
        AllocationRequest request = AllocationRequest.builder()
            .productId(1L)
            .warehouseId(1L)
            .quantity(50L)
            .build();
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L)).thenReturn(Optional.of(inventory));
        
        assertThrows(InsufficientStockException.class, () -> 
            allocationService.allocateFromWarehouse(request));
    }
    
    @Test
    @DisplayName("Should throw ProductNotFoundException when product not found")
    void testAllocateFromWarehouse_ProductNotFound() {
        AllocationRequest request = AllocationRequest.builder()
            .productId(999L)
            .warehouseId(1L)
            .quantity(50L)
            .build();
        
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(ProductNotFoundException.class, () -> 
            allocationService.allocateFromWarehouse(request));
    }
    
    @Test
    @DisplayName("Should throw WarehouseNotFoundException when warehouse not found")
    void testAllocateFromWarehouse_WarehouseNotFound() {
        AllocationRequest request = AllocationRequest.builder()
            .productId(1L)
            .warehouseId(999L)
            .quantity(50L)
            .build();
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(WarehouseNotFoundException.class, () -> 
            allocationService.allocateFromWarehouse(request));
    }
    
    @Test
    @DisplayName("Should confirm pending allocation")
    void testConfirmAllocation_Success() {
        allocation.setStatus(Allocation.AllocationStatus.PENDING);
        
        when(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(any(Allocation.class))).thenReturn(allocation);
        
        AllocationResponse response = allocationService.confirmAllocation(1L);
        
        assertNotNull(response);
        assertEquals(Allocation.AllocationStatus.CONFIRMED.toString(), response.getStatus());
        verify(allocationRepository).save(any(Allocation.class));
    }
    
    @Test
    @DisplayName("Should throw exception when confirming non-pending allocation")
    void testConfirmAllocation_InvalidState() {
        allocation.setStatus(Allocation.AllocationStatus.COMPLETED);
        
        when(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        
        assertThrows(InvalidStateException.class, () -> 
            allocationService.confirmAllocation(1L));
    }
    
    @Test
    @DisplayName("Should rollback pending allocation")
    void testRollbackAllocation_Success() {
        allocation.setStatus(Allocation.AllocationStatus.PENDING);
        
        when(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L)).thenReturn(Optional.of(inventory));
        when(allocationRepository.save(any(Allocation.class))).thenReturn(allocation);
        
        AllocationResponse response = allocationService.rollbackAllocation(1L);
        
        assertNotNull(response);
        assertEquals(Allocation.AllocationStatus.ROLLED_BACK.toString(), response.getStatus());
        verify(inventoryRepository).save(any(WarehouseInventory.class));
    }
    
    @Test
    @DisplayName("Should retrieve allocation by ID")
    void testGetAllocationById_Success() {
        when(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        
        AllocationResponse response = allocationService.getAllocationById(1L);
        
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(50L, response.getQuantity());
    }
    
    @Test
    @DisplayName("Should throw exception when allocation not found")
    void testGetAllocationById_NotFound() {
        when(allocationRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(AllocationNotFoundException.class, () -> 
            allocationService.getAllocationById(999L));
    }
}