package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Resumen de los filtros activos aplicados en una búsqueda de documentos (SRC-01 / HU-21, HU-22).
 *
 * <p>El campo completo {@code activeFilters} en {@link DocumentListResponse} es {@code null}
 * cuando no se aplica ningún filtro.
 */
@Schema(description = "Filtros activos aplicados en la búsqueda — SRC-01")
public record ActiveFiltersResponse(

        @Schema(description = "ID de la categoría aplicada como filtro", example = "3", nullable = true)
        Long categoryId,

        @Schema(description = "Nombre de la categoría aplicada como filtro", example = "Actas", nullable = true)
        String categoryName,

        @Schema(description = "Área responsable aplicada como filtro (búsqueda parcial)", example = "Rectoría", nullable = true)
        String responsibleArea,

        @Schema(description = "Fecha desde aplicada como filtro (YYYY-MM-DD)", example = "2026-01-01", nullable = true)
        LocalDate dateFrom,

        @Schema(description = "Fecha hasta aplicada como filtro (YYYY-MM-DD)", example = "2026-05-31", nullable = true)
        LocalDate dateTo,

        @Schema(description = "Nombre completo del usuario filtrado. Solo presente cuando el actor es ADMIN.", example = "María García López", nullable = true)
        String uploadedByName
) {
}
