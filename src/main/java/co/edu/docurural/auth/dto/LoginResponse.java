package co.edu.docurural.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body de {@code POST /api/auth/login} (AUTH-01).
 *
 * <p>El campo {@code tokenType} siempre es {@code "Bearer"}; {@code expiresIn}
 * se expresa en segundos.
 */
@Schema(description = "Respuesta de autenticación exitosa")
public record LoginResponse(
        @Schema(description = "Token JWT para incluir en Authorization: Bearer", example = "eyJhbGci...")
        String token,
        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,
        @Schema(description = "Segundos hasta expiración", example = "1800")
        long expiresIn,
        @Schema(description = "Datos del usuario autenticado")
        UserSummary user
) {

    public static LoginResponse bearer(String token, long expiresInSeconds, UserSummary user) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, user);
    }
}
