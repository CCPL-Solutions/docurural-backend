package co.edu.docurural.dashboard.dto;

import co.edu.docurural.document.enums.DocumentFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resumen de un documento reciente para el listado de los últimos 10 cargados (HU-25).
 */
@Schema(description = "Documento reciente mostrado en el listado del dashboard")
public record RecentDocumentResponse(

        @Schema(description = "Identificador único del documento", example = "47")
        Long id,

        @Schema(description = "Título del documento", example = "Acta Consejo Directivo Mayo 2026")
        String title,

        @Schema(description = "Nombre de la categoría", example = "Actas")
        String category,

        @Schema(description = "Área responsable", example = "Rectoría")
        String responsibleArea,

        @Schema(description = "Formato del archivo", example = "PDF")
        DocumentFormat fileFormat,

        @Schema(description = "Fecha y hora de carga al sistema (ISO 8601)", example = "2026-05-15T08:45:00")
        LocalDateTime createdAt
) {
}
