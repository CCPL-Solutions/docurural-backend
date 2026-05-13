package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Response body de {@code PUT /api/documents/{id}} (DOC-05 / HU-13).
 */
@Schema(description = "Documento con metadatos actualizados")
public record UpdateDocumentMetadataResponse(
        @Schema(description = "ID del documento", example = "47")
        Long id,
        @Schema(description = "Título actualizado", example = "Acta Consejo Directivo Marzo 2026 - Revisado")
        String title,
        @Schema(description = "Nombre de la categoría actual", example = "Actas")
        String category,
        @Schema(description = "Área responsable actual", example = "Rectoría")
        String responsibleArea,
        @Schema(description = "Fecha del documento", example = "2026-03-15")
        LocalDate documentDate,
        @Schema(description = "Mensaje de confirmación", example = "Documento actualizado exitosamente")
        String message
) {
}

