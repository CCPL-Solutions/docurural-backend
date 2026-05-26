package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body de {@code DELETE /api/documents/{id}} (DOC-06 / HU-14).
 */
@Schema(description = "Resultado de la eliminación lógica del documento")
public record DeleteDocumentResponseDto(
        @Schema(description = "ID del documento eliminado lógicamente", example = "47")
        Long id,
        @Schema(description = "Mensaje de confirmación", example = "Documento eliminado exitosamente")
        String message
) {
}
