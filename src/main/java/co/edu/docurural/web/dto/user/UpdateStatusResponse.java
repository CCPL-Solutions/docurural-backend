package co.edu.docurural.web.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body de {@code PATCH /api/users/{id}/status} (USR-05).
 */
@Schema(description = "Resultado del cambio de estado")
public record UpdateStatusResponse(
        @Schema(description = "ID del usuario", example = "1")
        Long id,
        @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
        String fullName,
        @Schema(description = "Correo electrónico del usuario", example = "juan.perez@docurural.com")
        String status,
        @Schema(description = "Mensaje de resultado", example = "Usuario habilitado exitosamente")
        String message
) {
}
