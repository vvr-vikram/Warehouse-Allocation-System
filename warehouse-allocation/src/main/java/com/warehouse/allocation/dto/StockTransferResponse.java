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
@Schema(description = "Response DTO for stock transfer")
public class StockTransferResponse {
    
    @Schema(description = "Transfer ID")
    private Long id;
    
    @Schema(description = "Source warehouse ID")
    private Long sourceWarehouseId;
    
    @Schema(description = "Source warehouse name")
    private String sourceWarehouseName;
    
    @Schema(description = "Target warehouse ID")
    private Long targetWarehouseId;
    
    @Schema(description = "Target warehouse name")
    private String targetWarehouseName;
    
    @Schema(description = "Product ID")
    private Long productId;
    
    @Schema(description = "Product name")
    private String productName;
    
    @Schema(description = "Transfer quantity")
    private Long quantity;
    
    @Schema(description = "Transfer status", example = "PENDING")
    private String status;
    
    @Schema(description = "Reference ID for tracking")
    private String referenceId;
    
    @Schema(description = "Transfer initiated timestamp")
    private LocalDateTime transferDate;
    
    @Schema(description = "Completed at timestamp")
    private LocalDateTime completedDate;
    
    @Schema(description = "Notes")
    private String notes;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}