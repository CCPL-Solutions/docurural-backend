package co.edu.docurural.web.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response body de {@code POST /api/users} (USR-03).
 *
 * <p>Extiende la información del usuario recién creado con un {@code message}
 * de confirmación, según el contrato documentado. No incluye {@code lastLogin}
 * porque en el momento de la creación siempre es {@code null}.
 */
@Schema(description = "Usuario recién creado")
public record CreateUserResponse(
        @Schema(description = "ID del usuario", example = "1")
        Long id,
        @Schema(description = "Nombre completo del usuario", example = "María García López")
        String fullName,
        @Schema(description = "Correo electrónico del usuario", example = "maria.garcia@docurural.edu.co")
        String email,
        @Schema(description = "Rol del usuario", example = "EDITOR")
        String role,
        @Schema(description = "Estado del usuario", example = "ACTIVE")
        String status,
        @Schema(description = "Fecha y hora de creación del usuario", example = "2024-06-01T12:34:56")
        LocalDateTime createdAt,
        @Schema(description = "Mensaje de confirmación", example = "Usuario creado exitosamente")
        String message
) {
}
