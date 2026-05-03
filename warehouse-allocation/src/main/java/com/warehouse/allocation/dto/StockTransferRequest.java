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
@Schema(description = "Request DTO for stock transfer")
public class StockTransferRequest {
    
    @NotNull(message = "Source warehouse ID is required")
    @Positive(message = "Source warehouse ID must be positive")
    @Schema(description = "Source warehouse ID", example = "1")
    private Long sourceWarehouseId;
    
    @NotNull(message = "Target warehouse ID is required")
    @Positive(message = "Target warehouse ID must be positive")
    @Schema(description = "Target warehouse ID", example = "2")
    private Long targetWarehouseId;
    
    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    @Schema(description = "Product ID", example = "1")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Schema(description = "Quantity to transfer", example = "50")
    private Long quantity;
    
    @Schema(description = "Notes for transfer", example = "Rebalancing inventory")
    private String notes;
}