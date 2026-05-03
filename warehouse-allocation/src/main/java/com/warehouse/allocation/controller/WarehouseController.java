package com.warehouse.allocation.controller;

import com.warehouse.allocation.dto.*;
import com.warehouse.allocation.service.WarehouseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;

@RestController
@RequestMapping("/warehouses")
@Validated
@Tag(name = "Warehouse Management", description = "APIs for managing warehouses")
@Slf4j
public class WarehouseController {
    
    private final WarehouseService warehouseService;
    
    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }
    
    @PostMapping
    @Operation(summary = "Create a new warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Warehouse created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Warehouse already exists")
    })
    public ResponseEntity<ResponseWrapper<WarehouseResponse>> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request) {
        log.info("Creating warehouse: {}", request.getName());
        WarehouseResponse response = warehouseService.createWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseWrapper.success(response, "Warehouse created successfully"));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Warehouse found"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    public ResponseEntity<ResponseWrapper<WarehouseResponse>> getWarehouse(
            @PathVariable @Positive Long id) {
        log.info("Fetching warehouse with ID: {}", id);
        WarehouseResponse response = warehouseService.getWarehouseById(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Warehouse retrieved successfully"));
    }
    
    @GetMapping
    @Operation(summary = "Get all active warehouses")
    @ApiResponse(responseCode = "200", description = "List of active warehouses")
    public ResponseEntity<ResponseWrapper<List<WarehouseResponse>>> getAllWarehouses() {
        log.info("Fetching all active warehouses");
        List<WarehouseResponse> warehouses = warehouseService.getAllActiveWarehouses();
        return ResponseEntity.ok(ResponseWrapper.success(warehouses, "Warehouses retrieved successfully"));
    }
    
    @GetMapping("/paginated")
    @Operation(summary = "Get warehouses with pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of warehouses")
    public ResponseEntity<ResponseWrapper<Page<WarehouseResponse>>> getWarehouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        log.info("Fetching warehouses with pagination: page={}, size={}", page, size);
        Page<WarehouseResponse> warehouses = warehouseService.getAllWarehouses(page, size, sortBy, direction);
        return ResponseEntity.ok(ResponseWrapper.success(warehouses, "Warehouses retrieved successfully"));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update warehouse details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Warehouse updated successfully"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ResponseWrapper<WarehouseResponse>> updateWarehouse(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CreateWarehouseRequest request) {
        log.info("Updating warehouse with ID: {}", id);
        WarehouseResponse response = warehouseService.updateWarehouse(id, request);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Warehouse updated successfully"));
    }
    
    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Warehouse activated"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    public ResponseEntity<ResponseWrapper<WarehouseResponse>> activateWarehouse(
            @PathVariable @Positive Long id) {
        log.info("Activating warehouse with ID: {}", id);
        WarehouseResponse response = warehouseService.activateWarehouse(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Warehouse activated successfully"));
    }
    
    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Warehouse deactivated"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    public ResponseEntity<ResponseWrapper<WarehouseResponse>> deactivateWarehouse(
            @PathVariable @Positive Long id) {
        log.info("Deactivating warehouse with ID: {}", id);
        WarehouseResponse response = warehouseService.deactivateWarehouse(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Warehouse deactivated successfully"));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Warehouse deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    public ResponseEntity<Void> deleteWarehouse(
            @PathVariable @Positive Long id) {
        log.info("Deleting warehouse with ID: {}", id);
        warehouseService.softDeleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}