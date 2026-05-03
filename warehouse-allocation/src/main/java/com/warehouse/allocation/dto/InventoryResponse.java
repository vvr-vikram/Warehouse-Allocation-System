package com.warehouse.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO for warehouse inventory")
public class InventoryResponse {
    
    @Schema(description = "Inventory ID")
    private Long id;
    
    @Schema(description = "Warehouse ID")
    private Long warehouseId;
    
    @Schema(description = "Warehouse name")
    private String warehouseName;
    
    @Schema(description = "Product ID")
    private Long productId;
    
    @Schema(description = "Product name")
    private String productName;
    
    @Schema(description = "Available quantity")
    private Long availableQuantity;
    
    @Schema(description = "Version for optimistic locking")
    private Long version;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}