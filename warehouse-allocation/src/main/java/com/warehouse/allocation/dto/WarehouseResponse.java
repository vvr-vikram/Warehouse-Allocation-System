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
@Schema(description = "Response DTO for warehouse")
public class WarehouseResponse {
    
    @Schema(description = "Warehouse ID")
    private Long id;
    
    @Schema(description = "Warehouse name")
    private String name;
    
    @Schema(description = "Warehouse location")
    private String location;
    
    @Schema(description = "Storage capacity")
    private Long capacity;
    
    @Schema(description = "Warehouse status", example = "ACTIVE")
    private String status;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}