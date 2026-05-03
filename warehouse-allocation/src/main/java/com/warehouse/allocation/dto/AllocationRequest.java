package com.warehouse.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for allocating stock")
public class AllocationRequest {
    
    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    @Schema(description = "Product ID", example = "1")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Schema(description = "Quantity to allocate", example = "100")
    private Long quantity;
    
    @Schema(description = "Warehouse ID (optional - auto-select if not provided)", example = "1")
    private Long warehouseId;
    
    @Schema(description = "Notes for allocation", example = "Urgent order")
    private String notes;
}