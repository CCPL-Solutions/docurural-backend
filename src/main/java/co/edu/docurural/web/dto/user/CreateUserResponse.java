package co.edu.docurural.web.dto.user;

import java.time.LocalDateTime;

/**
 * Response body de {@code POST /api/users} (USR-03).
 *
 * <p>Extiende la información del usuario recién creado con un {@code message}
 * de confirmación, según el contrato documentado. No incluye {@code lastLogin}
 * porque en el momento de la creación siempre es {@code null}.
 */
public record CreateUserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        String status,
        LocalDateTime createdAt,
        String message
) {
}
