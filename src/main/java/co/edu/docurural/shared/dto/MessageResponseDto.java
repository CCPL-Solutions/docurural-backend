package co.edu.docurural.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO genérico para respuestas que solo contienen un mensaje de confirmación.
 */
@Schema(description = "Respuesta genérica de confirmación")
public record MessageResponseDto(
        @Schema(description = "Mensaje de respuesta", example = "Operación realizada correctamente") String message
) {
}
