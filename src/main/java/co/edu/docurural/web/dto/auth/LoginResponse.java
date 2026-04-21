package co.edu.docurural.web.dto.auth;

/**
 * Response body de {@code POST /api/auth/login} (AUTH-01).
 *
 * <p>El campo {@code tokenType} siempre es {@code "Bearer"}; {@code expiresIn}
 * se expresa en segundos.
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserSummary user
) {

    public static LoginResponse bearer(String token, long expiresInSeconds, UserSummary user) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, user);
    }
}
