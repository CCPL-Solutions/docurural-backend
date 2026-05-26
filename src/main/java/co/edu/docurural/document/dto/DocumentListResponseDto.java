package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Envelope de la respuesta paginada del listado y búsqueda de documentos activos
 * (DOC-01 / HU-15, HU-20, HU-21, HU-22).
 *
 * <p>{@code searchTerm} y {@code activeFilters} son {@code null} cuando no se
 * proporciona ningún criterio de búsqueda o filtrado.
 */
@Schema(description = "Respuesta paginada del listado o búsqueda de documentos — DOC-01 / SRC-01")
public record DocumentListResponseDto(

        @Schema(description = "Total de documentos que cumplen los criterios de búsqueda", example = "47")
        Integer totalDocuments,

        @Schema(description = "Total de páginas dado el tamaño solicitado", example = "3")
        Integer totalPages,

        @Schema(description = "Número de la página actual (1-based)", example = "1")
        Integer currentPage,

        @Schema(description = "Tamaño de página aplicado", example = "20")
        Integer pageSize,

        @Schema(description = "Término de búsqueda por texto libre aplicado. Null si no se realizó búsqueda por texto.", example = "acta", nullable = true)
        String searchTerm,

        @Schema(description = "Filtros estructurados activos. Null si no se aplicaron filtros.", nullable = true)
        ActiveFiltersResponseDto activeFilters,

        @Schema(description = "Lista de documentos en la página actual")
        List<DocumentSummaryResponseDto> documents
) {
}
