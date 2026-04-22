package co.edu.docurural.web.dto.auth;

/**
 * Datos básicos del usuario autenticado incluidos en {@link LoginResponse}.
 *
 * <p>Coincide con el contrato AUTH-01: {@code id}, {@code fullName},
 * {@code email}, {@code role}.
 */
public record UserSummary(
        Long id,
        String fullName,
        String email,
        String role
) {
}
