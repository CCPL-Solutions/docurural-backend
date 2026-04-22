package co.edu.docurural.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body de {@code POST /api/auth/login} (AUTH-01).
 */
public record LoginRequest(

        @NotBlank(message = "{validation.user.email.required}")
        @Email(message = "{validation.user.email.format}")
        String email,

        @NotBlank(message = "{validation.user.password.required}")
        String password
) {
}
