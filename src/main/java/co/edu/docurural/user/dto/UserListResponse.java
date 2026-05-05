package co.edu.docurural.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response body de {@code GET /api/users} (USR-01).
 *
 * <p>Contiene el total de usuarios y la lista completa (sin paginación en el
 * MVP, según nota de implementación de USR-01).
 */
@Schema(description = "Listado de usuarios del sistema")
public record UserListResponse(
        @Schema(description = "Total de usuarios activos e inactivos", example = "12")
        long totalUsers,
        @Schema(description = "Lista ordenada de usuarios")
        List<UserResponse> users
) {
}
