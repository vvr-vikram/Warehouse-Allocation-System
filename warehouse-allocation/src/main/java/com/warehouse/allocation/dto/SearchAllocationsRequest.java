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
@Schema(description = "Search allocations request")
public class SearchAllocationsRequest {
    
    @Schema(description = "Product ID to filter by")
    private Long productId;
    
    @Schema(description = "Warehouse ID to filter by")
    private Long warehouseId;
    
    @Schema(description = "Allocation status to filter by")
    private String status;
    
    @Schema(description = "Start date for allocation date range")
    private LocalDateTime startDate;
    
    @Schema(description = "End date for allocation date range")
    private LocalDateTime endDate;
    
    @Schema(description = "Page number (0-indexed)", example = "0")
    private Integer page = 0;
    
    @Schema(description = "Page size", example = "20")
    private Integer size = 20;
    
    @Schema(description = "Sort field", example = "createdAt")
    private String sortBy = "createdAt";
    
    @Schema(description = "Sort direction", example = "DESC")
    private String direction = "DESC";
}