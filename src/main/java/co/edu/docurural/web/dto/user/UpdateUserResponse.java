package co.edu.docurural.web.dto.user;

/**
 * Response body de {@code PUT /api/users/{id}} (USR-04).
 */
public record UpdateUserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        String status,
        String message
) {
}
