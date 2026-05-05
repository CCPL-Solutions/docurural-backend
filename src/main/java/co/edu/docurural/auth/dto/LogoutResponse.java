package co.edu.docurural.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body de {@code POST /api/auth/logout} (AUTH-02).
 */
@Schema(description = "Confirmación de cierre de sesión")
public record LogoutResponse(
        @Schema(description = "Mensaje de confirmación", example = "Sesión cerrada exitosamente")
        String message
) {
}
