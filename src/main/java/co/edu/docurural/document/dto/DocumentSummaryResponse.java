package co.edu.docurural.document.dto;

import co.edu.docurural.document.enums.DocumentFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila del listado paginado de documentos (DOC-01 / HU-15).
 */
@Schema(description = "Documento (resumen) — DOC-01")
public record DocumentSummaryResponse(
        @Schema(description = "Identificador único", example = "47")
        Long id,

        @Schema(description = "Título del documento", example = "Acta Consejo Directivo Marzo 2026")
        String title,

        @Schema(description = "Nombre de la categoría asignada", example = "Actas")
        String category,

        @Schema(description = "Área de la institución responsable", example = "Rectoría")
        String responsibleArea,

        @Schema(description = "Fecha del documento (YYYY-MM-DD)", example = "2026-03-15")
        LocalDate documentDate,

        @Schema(description = "Formato del archivo", example = "PDF")
        DocumentFormat fileFormat,

        @Schema(description = "Tamaño del archivo en bytes (el frontend lo formatea)", example = "524288")
        Long fileSizeBytes,

        @Schema(description = "Nombre completo del usuario que cargó el documento", example = "María García López")
        String uploadedBy,

        @Schema(description = "Fecha y hora de carga al sistema (ISO 8601)", example = "2026-04-10T09:30:00")
        LocalDateTime createdAt
) {
}
