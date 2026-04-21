package co.edu.docurural.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body de {@code POST /api/auth/login} (AUTH-01).
 */
public record LoginRequest(

        @NotBlank(message = "El correo electronico es obligatorio")
        @Email(message = "El correo electronico no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}
