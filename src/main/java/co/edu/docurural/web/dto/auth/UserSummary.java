package co.edu.docurural.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datos básicos del usuario autenticado incluidos en {@link LoginResponse}.
 *
 * <p>Coincide con el contrato AUTH-01: {@code id}, {@code fullName},
 * {@code email}, {@code role}.
 */
@Schema(description = "Datos básicos del usuario autenticado")
public record UserSummary(
        @Schema(description = "Identificador único del usuario", example = "123")
        Long id,
        @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
        String fullName,
        @Schema(description = "Correo electrónico del usuario", example = "correo@gmail.com")
        String email,
        @Schema(description = "Rol del usuario", example = "ADMIN")
        String role
) {
}
