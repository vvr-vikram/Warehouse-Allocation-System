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
@Schema(description = "Response DTO for allocation")
public class AllocationResponse {
    
    @Schema(description = "Allocation ID")
    private Long id;
    
    @Schema(description = "Product ID")
    private Long productId;
    
    @Schema(description = "Product name")
    private String productName;
    
    @Schema(description = "Warehouse ID")
    private Long warehouseId;
    
    @Schema(description = "Warehouse name")
    private String warehouseName;
    
    @Schema(description = "Allocated quantity")
    private Long quantity;
    
    @Schema(description = "Allocation status", example = "CONFIRMED")
    private String status;
    
    @Schema(description = "Reference ID for tracking")
    private String referenceId;
    
    @Schema(description = "Allocated at timestamp")
    private LocalDateTime allocatedAt;
    
    @Schema(description = "Confirmed at timestamp")
    private LocalDateTime confirmedAt;
    
    @Schema(description = "Completed at timestamp")
    private LocalDateTime completedAt;
    
    @Schema(description = "Notes")
    private String notes;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}