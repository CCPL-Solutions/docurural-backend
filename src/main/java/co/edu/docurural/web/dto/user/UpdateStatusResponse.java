package co.edu.docurural.web.dto.user;

/**
 * Response body de {@code PATCH /api/users/{id}/status} (USR-05).
 */
public record UpdateStatusResponse(
        Long id,
        String fullName,
        String status,
        String message
) {
}
