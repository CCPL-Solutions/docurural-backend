package co.edu.docurural.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listado de categorías con resumen de estados")
public record CategoryListResponse(
        @Schema(description = "Total de categorías (ACTIVE + INACTIVE)", example = "10") Integer totalCategories,
        @Schema(description = "Cantidad con estado ACTIVE", example = "8") Integer activeCategories,
        @Schema(description = "Cantidad con estado INACTIVE", example = "2") Integer inactiveCategories,
        @Schema(description = "Lista ordenada de categorías") List<CategoryDetailResponse> categories
) {
}
