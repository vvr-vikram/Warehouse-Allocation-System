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
@Schema(description = "Response DTO for product")
public class ProductResponse {
    
    @Schema(description = "Product ID")
    private Long id;
    
    @Schema(description = "Product name")
    private String name;
    
    @Schema(description = "SKU")
    private String sku;
    
    @Schema(description = "Total stock across all warehouses")
    private Long totalStock;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}