package com.warehouse.allocation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.warehouse.allocation.dto.ResponseWrapper;
import com.warehouse.allocation.dto.InventoryResponse;
import com.warehouse.allocation.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/inventory")
@Validated
@Tag(name = "Inventory Management", description = "APIs for managing warehouse inventory")
@Slf4j
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    @GetMapping("/{warehouseId}/{productId}")
    @Operation(summary = "Get inventory for warehouse-product combination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory found"),
        @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    public ResponseEntity<ResponseWrapper<InventoryResponse>> getInventory(
            @PathVariable @Positive Long warehouseId,
            @PathVariable @Positive Long productId) {
        log.info("Fetching inventory for warehouse {} and product {}", warehouseId, productId);
        InventoryResponse response = inventoryService.getInventory(warehouseId, productId);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Inventory retrieved successfully"));
    }
    
    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get all inventory for a warehouse")
    @ApiResponse(responseCode = "200", description = "List of inventory")
    public ResponseEntity<ResponseWrapper<List<InventoryResponse>>> getWarehouseInventory(
            @PathVariable @Positive Long warehouseId) {
        log.info("Fetching inventory for warehouse: {}", warehouseId);
        List<InventoryResponse> inventory = inventoryService.getWarehouseInventory(warehouseId);
        return ResponseEntity.ok(ResponseWrapper.success(inventory, "Warehouse inventory retrieved successfully"));
    }
    
    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory for product across all warehouses")
    @ApiResponse(responseCode = "200", description = "List of inventory")
    public ResponseEntity<ResponseWrapper<List<InventoryResponse>>> getProductInventory(
            @PathVariable @Positive Long productId) {
        log.info("Fetching inventory for product: {}", productId);
        List<InventoryResponse> inventory = inventoryService.getProductInventory(productId);
        return ResponseEntity.ok(ResponseWrapper.success(inventory, "Product inventory retrieved successfully"));
    }
    
    @GetMapping("/product/{productId}/total")
    @Operation(summary = "Get total available stock for a product")
    @ApiResponse(responseCode = "200", description = "Total stock quantity")
    public ResponseEntity<ResponseWrapper<Long>> getTotalAvailableStock(
            @PathVariable @Positive Long productId) {
        log.info("Fetching total available stock for product: {}", productId);
        Long totalStock = inventoryService.getTotalAvailableStock(productId);
        return ResponseEntity.ok(ResponseWrapper.success(totalStock, "Total stock retrieved successfully"));
    }
    
    @PostMapping("/{warehouseId}/{productId}")
    @Operation(summary = "Initialize inventory for warehouse-product combination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Inventory initialized"),
        @ApiResponse(responseCode = "400", description = "Invalid input or already exists"),
        @ApiResponse(responseCode = "404", description = "Warehouse or product not found")
    })
    public ResponseEntity<ResponseWrapper<InventoryResponse>> initializeInventory(
            @PathVariable @Positive Long warehouseId,
            @PathVariable @Positive Long productId,
            @RequestParam @Positive Long quantity) {
        log.info("Initializing inventory for warehouse {} and product {} with quantity {}", 
            warehouseId, productId, quantity);
        InventoryResponse response = inventoryService.initializeInventory(warehouseId, productId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseWrapper.success(response, "Inventory initialized successfully"));
    }
}