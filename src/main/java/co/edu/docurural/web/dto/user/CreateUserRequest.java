package co.edu.docurural.web.dto.user;

import co.edu.docurural.domain.enums.enums.UserRole;
import co.edu.docurural.web.dto.validation.PasswordsMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body de {@code POST /api/users} (USR-03).
 *
 * <p>La validación cruzada {@code password} / {@code confirmPassword} se
 * delega en {@link PasswordsMatch}; {@code @NotBlank} en {@code password}
 * garantiza que para esta operación la contraseña sea obligatoria.
 */
@PasswordsMatch
public record CreateUserRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String fullName,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 150, message = "El correo electrónico no puede exceder 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
        String password,

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        String confirmPassword,

        @NotNull(message = "El rol es obligatorio")
        UserRole role
) {
}
