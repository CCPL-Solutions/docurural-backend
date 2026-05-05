package co.edu.docurural.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body de {@code POST /api/auth/login} (AUTH-01).
 */
@Schema(description = "Credenciales de acceso")
public record LoginRequest(
        @Schema(description = "Correo electrónico registrado", example = "admin@docurural.edu.co")
        @NotBlank @Email String email,
        @Schema(description = "Contraseña del usuario", example = "Admin1234!")
        @NotBlank String password
) {
}
