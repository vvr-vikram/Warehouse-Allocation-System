package com.warehouse.allocation.controller;

import com.warehouse.allocation.dto.ResponseWrapper;
import com.warehouse.allocation.dto.AllocationRequest;
import com.warehouse.allocation.dto.AllocationResponse;
import com.warehouse.allocation.dto.PaginatedResponse;
import com.warehouse.allocation.dto.SearchAllocationsRequest;
import com.warehouse.allocation.service.AllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/allocations")
@Validated
@Tag(name = "Allocation Management", description = "APIs for managing stock allocations")
@Slf4j
public class AllocationController {
    
    private final AllocationService allocationService;
    
    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }
    
    @PostMapping
    @Operation(summary = "Allocate stock from a specific warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Allocation created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or insufficient stock"),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found"),
        @ApiResponse(responseCode = "409", description = "Concurrent modification conflict")
    })
    public ResponseEntity<ResponseWrapper<AllocationResponse>> allocateStock(
            @Valid @RequestBody AllocationRequest request) {
        log.info("Allocating stock: productId={}, quantity={}, warehouseId={}",
            request.getProductId(), request.getQuantity(), request.getWarehouseId());
        
        AllocationResponse response = allocationService.allocateFromWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseWrapper.success(response, "Stock allocated successfully"));
    }
    
    @PostMapping("/auto-allocate")
    @Operation(summary = "Auto-allocate stock from best available warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Allocation created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or no warehouse with sufficient stock"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ResponseWrapper<AllocationResponse>> autoAllocateStock(
            @Valid @RequestBody AllocationRequest request) {
        log.info("Auto-allocating stock: productId={}, quantity={}",
            request.getProductId(), request.getQuantity());
        
        // Clear warehouse ID for auto-selection
        request.setWarehouseId(null);
        AllocationResponse response = allocationService.allocateFromBestWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseWrapper.success(response, "Stock auto-allocated successfully"));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get allocation by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allocation found"),
        @ApiResponse(responseCode = "404", description = "Allocation not found")
    })
    public ResponseEntity<ResponseWrapper<AllocationResponse>> getAllocationById(
            @PathVariable @Positive Long id) {
        log.info("Fetching allocation with ID: {}", id);
        AllocationResponse response = allocationService.getAllocationById(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Allocation retrieved successfully"));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search allocations with filters")
    @ApiResponse(responseCode = "200", description = "Search results")
    public ResponseEntity<ResponseWrapper<PaginatedResponse<AllocationResponse>>> searchAllocations(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "allocatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        
        log.info("Searching allocations with filters: productId={}, warehouseId={}, status={}",
            productId, warehouseId, status);
        
        // Parse dates if provided
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startDate != null && !startDate.isEmpty()) {
            startDateTime = LocalDateTime.parse(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            endDateTime = LocalDateTime.parse(endDate);
        }
        
        SearchAllocationsRequest searchRequest = SearchAllocationsRequest.builder()
            .productId(productId)
            .warehouseId(warehouseId)
            .status(status)
            .startDate(startDateTime)
            .endDate(endDateTime)
            .page(page)
            .size(size)
            .sortBy(sortBy)
            .direction(direction)
            .build();
        
        PaginatedResponse<AllocationResponse> paginatedResponse = allocationService.searchAllocations(searchRequest);
        return ResponseEntity.ok(ResponseWrapper.success(paginatedResponse, "Allocations retrieved successfully"));
    }
    
    @GetMapping("/product/{productId}")
    @Operation(summary = "Get all allocations for a product")
    @ApiResponse(responseCode = "200", description = "List of allocations")
    public ResponseEntity<ResponseWrapper<List<AllocationResponse>>> getAllocationsForProduct(
            @PathVariable @Positive Long productId) {
        log.info("Fetching allocations for product: {}", productId);
        List<AllocationResponse> response = allocationService.getAllocationsForProduct(productId);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Allocations retrieved successfully"));
    }
    
    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get all allocations for a warehouse")
    @ApiResponse(responseCode = "200", description = "List of allocations")
    public ResponseEntity<ResponseWrapper<List<AllocationResponse>>> getAllocationsForWarehouse(
            @PathVariable @Positive Long warehouseId) {
        log.info("Fetching allocations for warehouse: {}", warehouseId);
        List<AllocationResponse> response = allocationService.getAllocationsForWarehouse(warehouseId);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Allocations retrieved successfully"));
    }
    
    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm a pending allocation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allocation confirmed"),
        @ApiResponse(responseCode = "404", description = "Allocation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid allocation state")
    })
    public ResponseEntity<ResponseWrapper<AllocationResponse>> confirmAllocation(
            @PathVariable @Positive Long id) {
        log.info("Confirming allocation with ID: {}", id);
        AllocationResponse response = allocationService.confirmAllocation(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Allocation confirmed successfully"));
    }
    
    @PatchMapping("/{id}/rollback")
    @Operation(summary = "Rollback a pending allocation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allocation rolled back"),
        @ApiResponse(responseCode = "404", description = "Allocation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid allocation state")
    })
    public ResponseEntity<ResponseWrapper<AllocationResponse>> rollbackAllocation(
            @PathVariable @Positive Long id) {
        log.info("Rolling back allocation with ID: {}", id);
        AllocationResponse response = allocationService.rollbackAllocation(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Allocation rolled back successfully"));
    }
}