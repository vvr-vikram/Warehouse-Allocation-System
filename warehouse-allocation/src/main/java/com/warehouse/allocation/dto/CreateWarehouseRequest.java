package com.warehouse.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for creating a warehouse")
public class CreateWarehouseRequest {
    
    @NotBlank(message = "Warehouse name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String name;
    
    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 255, message = "Location must be between 2 and 255 characters")
    @Schema(description = "Warehouse location", example = "New York")
    private String location;
    
    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive")
    @Schema(description = "Warehouse storage capacity", example = "100000")
    private Long capacity;
}