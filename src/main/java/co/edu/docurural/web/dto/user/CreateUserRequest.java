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
 * <p>La validacion cruzada {@code password} / {@code confirmPassword} se
 * delega en {@link PasswordsMatch}; {@code @NotBlank} en {@code password}
 * garantiza que para esta operacion la contrasena sea obligatoria.
 */
@PasswordsMatch
public record CreateUserRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String fullName,

        @NotBlank(message = "El correo electronico es obligatorio")
        @Email(message = "El correo electronico no tiene un formato valido")
        @Size(max = 150, message = "El correo electronico no puede exceder 150 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener minimo 8 caracteres")
        String password,

        @NotBlank(message = "La confirmacion de contrasena es obligatoria")
        String confirmPassword,

        @NotNull(message = "El rol es obligatorio")
        UserRole role
) {
}
