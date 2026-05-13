package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Envelope de la respuesta paginada del listado de documentos activos (DOC-01 / HU-15).
 */
@Schema(description = "Respuesta paginada del listado de documentos — DOC-01")
public record DocumentListResponse(
        @Schema(description = "Total de documentos activos en el sistema", example = "47")
        Integer totalDocuments,

        @Schema(description = "Total de páginas dado el tamaño solicitado", example = "3")
        Integer totalPages,

        @Schema(description = "Número de la página actual (1-based)", example = "1")
        Integer currentPage,

        @Schema(description = "Tamaño de página aplicado", example = "20")
        Integer pageSize,

        @Schema(description = "Lista de documentos en la página actual")
        List<DocumentSummaryResponse> documents
) {
}
