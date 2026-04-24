package co.edu.docurural.web.dto.user;

import java.util.List;

/**
 * Response body de {@code GET /api/users} (USR-01).
 *
 * <p>Contiene el total de usuarios y la lista completa (sin paginación en el
 * MVP, según nota de implementación de USR-01).
 */
public record UserListResponse(
        long totalUsers,
        List<UserResponse> users
) {
}
