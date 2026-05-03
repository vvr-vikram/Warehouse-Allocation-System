package com.warehouse.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated response wrapper")
public class PaginatedResponse<T> {
    
    @Schema(description = "Content list")
    private List<T> content;
    
    @Schema(description = "Current page number")
    private Integer page;
    
    @Schema(description = "Page size")
    private Integer size;
    
    @Schema(description = "Total elements")
    private Long totalElements;
    
    @Schema(description = "Total pages")
    private Integer totalPages;
    
    @Schema(description = "Is last page")
    private Boolean isLast;
    
    public static <T> PaginatedResponse<T> empty() {
        return PaginatedResponse.<T>builder()
            .content(List.of())
            .page(0)
            .size(0)
            .totalElements(0L)
            .totalPages(0)
            .isLast(true)
            .build();
    }
}