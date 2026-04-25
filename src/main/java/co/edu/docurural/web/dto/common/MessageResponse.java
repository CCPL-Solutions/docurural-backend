package co.edu.docurural.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO genérico para respuestas que solo contienen un mensaje de confirmación.
 */
@Schema(description = "Respuesta genérica de confirmación")
public record MessageResponse(
        @Schema(description = "Mensaje de respuesta", example = "Operación realizada correctamente") String message
) {
}
