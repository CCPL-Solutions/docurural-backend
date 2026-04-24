package co.edu.docurural.web.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body de {@code PUT /api/users/{id}} (USR-04).
 */
@Schema(description = "Usuario actualizado")
public record UpdateUserResponse(
        @Schema(description = "ID del usuario", example = "5")
        Long id,
        @Schema(description = "Nombre completo", example = "María García López")
        String fullName,
        @Schema(description = "Email único en el sistema", example = "maria.garcia@docurural.edu.co")
        String email,
        @Schema(description = "Rol asignado: ADMIN | EDITOR | READER", example = "READER")
        String role,
        @Schema(description = "Estado del usuario", example = "ACTIVE")
        String status,
        @Schema(description = "Mensaje de confirmación", example = "Usuario actualizado correctamente")
        String message
) {
}
