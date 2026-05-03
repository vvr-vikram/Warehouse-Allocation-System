package com.warehouse.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for creating a product")
public class CreateProductRequest {
    
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Product name", example = "Laptop")
    private String name;
    
    @NotBlank(message = "SKU is required")
    @Size(min = 2, max = 100, message = "SKU must be between 2 and 100 characters")
    @Schema(description = "Stock Keeping Unit", example = "LAPTOP-001")
    private String sku;
    
    @NotNull(message = "Total stock is required")
    @PositiveOrZero(message = "Total stock cannot be negative")
    @Schema(description = "Total available stock", example = "500")
    private Long totalStock;
}