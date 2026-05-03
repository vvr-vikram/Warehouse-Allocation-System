package com.warehouse.allocation.controller;

import com.warehouse.allocation.dto.*;
import com.warehouse.allocation.service.StockTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/transfers")
@Validated
@Tag(name = "Stock Transfer", description = "APIs for managing stock transfers between warehouses")
@Slf4j
public class StockTransferController {
    
    private final StockTransferService stockTransferService;
    
    public StockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }
    
    @PostMapping
    @Operation(summary = "Initiate a stock transfer between warehouses")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transfer initiated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or insufficient stock"),
        @ApiResponse(responseCode = "404", description = "Warehouse or product not found")
    })
    public ResponseEntity<ResponseWrapper<StockTransferResponse>> initiateTransfer(
            @Valid @RequestBody StockTransferRequest request) {
        log.info("Initiating stock transfer from warehouse {} to warehouse {} for product {}",
            request.getSourceWarehouseId(), request.getTargetWarehouseId(), request.getProductId());
        
        StockTransferResponse response = stockTransferService.initiateTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseWrapper.success(response, "Stock transfer initiated successfully"));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get transfer by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transfer found"),
        @ApiResponse(responseCode = "404", description = "Transfer not found")
    })
    public ResponseEntity<ResponseWrapper<StockTransferResponse>> getTransferById(
            @PathVariable @Positive Long id) {
        log.info("Fetching transfer with ID: {}", id);
        StockTransferResponse response = stockTransferService.getTransferById(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Transfer retrieved successfully"));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search transfers with filters")
    @ApiResponse(responseCode = "200", description = "Search results")
    public ResponseEntity<ResponseWrapper<Page<StockTransferResponse>>> searchTransfers(
            @RequestParam(required = false) Long sourceWarehouseId,
            @RequestParam(required = false) Long targetWarehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        
        log.info("Searching transfers with filters: sourceWarehouse={}, targetWarehouse={}, product={}, status={}",
            sourceWarehouseId, targetWarehouseId, productId, status);
        
        Page<StockTransferResponse> response = stockTransferService.searchTransfers(
            sourceWarehouseId, targetWarehouseId, productId, status, page, size, sortBy, direction);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Transfers retrieved successfully"));
    }
    
    @PatchMapping("/{id}/execute")
    @Operation(summary = "Execute a pending transfer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transfer executed successfully"),
        @ApiResponse(responseCode = "404", description = "Transfer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid transfer state or insufficient stock"),
        @ApiResponse(responseCode = "409", description = "Concurrent modification")
    })
    public ResponseEntity<ResponseWrapper<StockTransferResponse>> executeTransfer(
            @PathVariable @Positive Long id) {
        log.info("Executing transfer with ID: {}", id);
        StockTransferResponse response = stockTransferService.executeTransfer(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Transfer executed successfully"));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a pending transfer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Transfer cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Transfer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid transfer state")
    })
    public ResponseEntity<Void> cancelTransfer(
            @PathVariable @Positive Long id) {
        log.info("Cancelling transfer with ID: {}", id);
        stockTransferService.cancelTransfer(id);
        return ResponseEntity.noContent().build();
    }
}