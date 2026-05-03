package com.warehouse.allocation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.warehouse.allocation.dto.CreateWarehouseRequest;
import com.warehouse.allocation.dto.WarehouseResponse;
import com.warehouse.allocation.entity.Warehouse;
import com.warehouse.allocation.exception.AllocationException.WarehouseNotFoundException;
import com.warehouse.allocation.repository.WarehouseRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseService Tests")
class WarehouseServiceTest {
    
    @Mock
    private WarehouseRepository warehouseRepository;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private WarehouseService warehouseService;
    
    private Warehouse warehouse;
    private CreateWarehouseRequest request;
    
    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
            .id(1L)
            .name("Main Warehouse")
            .location("New York")
            .capacity(100000L)
            .status(Warehouse.WarehouseStatus.ACTIVE)
            .isDeleted(false)
            .build();
        
        request = CreateWarehouseRequest.builder()
            .name("Main Warehouse")
            .location("New York")
            .capacity(100000L)
            .build();
    }
    
    @Test
    @DisplayName("Should create warehouse successfully")
    void testCreateWarehouse_Success() {
        when(warehouseRepository.findByNameAndIsDeletedFalse("Main Warehouse")).thenReturn(Optional.empty());
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);
        
        WarehouseResponse response = warehouseService.createWarehouse(request);
        
        assertNotNull(response);
        assertEquals("Main Warehouse", response.getName());
        assertEquals("New York", response.getLocation());
        assertEquals(100000L, response.getCapacity());
        verify(warehouseRepository).save(any(Warehouse.class));
        verify(auditService).logAction(anyString(), anyLong(), any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when warehouse name already exists")
    void testCreateWarehouse_DuplicateName() {
        when(warehouseRepository.findByNameAndIsDeletedFalse("Main Warehouse"))
            .thenReturn(Optional.of(warehouse));
        
        assertThrows(IllegalArgumentException.class, () -> 
            warehouseService.createWarehouse(request));
    }
    
    @Test
    @DisplayName("Should retrieve warehouse by ID")
    void testGetWarehouseById_Success() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        
        WarehouseResponse response = warehouseService.getWarehouseById(1L);
        
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Main Warehouse", response.getName());
    }
    
    @Test
    @DisplayName("Should throw exception when warehouse not found")
    void testGetWarehouseById_NotFound() {
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(WarehouseNotFoundException.class, () -> 
            warehouseService.getWarehouseById(999L));
    }
    
    @Test
    @DisplayName("Should update warehouse successfully")
    void testUpdateWarehouse_Success() {
        CreateWarehouseRequest updateRequest = CreateWarehouseRequest.builder()
            .name("Main Warehouse Updated")
            .location("Boston")
            .capacity(150000L)
            .build();
        
        Warehouse updatedWarehouse = Warehouse.builder()
            .id(1L)
            .name("Main Warehouse Updated")
            .location("Boston")
            .capacity(150000L)
            .status(Warehouse.WarehouseStatus.ACTIVE)
            .isDeleted(false)
            .build();
        
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.findByNameAndIsDeletedFalse("Main Warehouse Updated"))
            .thenReturn(Optional.empty());
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(updatedWarehouse);
        
        WarehouseResponse response = warehouseService.updateWarehouse(1L, updateRequest);
        
        assertNotNull(response);
        assertEquals("Main Warehouse Updated", response.getName());
        assertEquals("Boston", response.getLocation());
        verify(warehouseRepository).save(any(Warehouse.class));
    }
    
    @Test
    @DisplayName("Should activate warehouse")
    void testActivateWarehouse_Success() {
        warehouse.setStatus(Warehouse.WarehouseStatus.INACTIVE);
        
        Warehouse activatedWarehouse = Warehouse.builder()
            .id(1L)
            .name("Main Warehouse")
            .location("New York")
            .capacity(100000L)
            .status(Warehouse.WarehouseStatus.ACTIVE)
            .isDeleted(false)
            .build();
        
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(activatedWarehouse);
        
        WarehouseResponse response = warehouseService.activateWarehouse(1L);
        
        assertNotNull(response);
        assertEquals("ACTIVE", response.getStatus());
        verify(warehouseRepository).save(any(Warehouse.class));
    }
    
    @Test
    @DisplayName("Should deactivate warehouse")
    void testDeactivateWarehouse_Success() {
        Warehouse inactiveWarehouse = Warehouse.builder()
            .id(1L)
            .name("Main Warehouse")
            .location("New York")
            .capacity(100000L)
            .status(Warehouse.WarehouseStatus.INACTIVE)
            .isDeleted(false)
            .build();
        
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(inactiveWarehouse);
        
        WarehouseResponse response = warehouseService.deactivateWarehouse(1L);
        
        assertNotNull(response);
        assertEquals("INACTIVE", response.getStatus());
        verify(warehouseRepository).save(any(Warehouse.class));
    }
    
    @Test
    @DisplayName("Should soft delete warehouse")
    void testSoftDeleteWarehouse_Success() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        
        warehouseService.softDeleteWarehouse(1L);
        
        verify(warehouseRepository).save(any(Warehouse.class));
        verify(auditService).logAction(anyString(), anyLong(), any(), any(), any(), anyString());
    }
}