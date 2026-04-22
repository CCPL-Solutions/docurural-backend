package co.edu.docurural.web.dto.user;

import co.edu.docurural.domain.enums.enums.UserRole;
import co.edu.docurural.web.dto.validation.PasswordsMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body de {@code PUT /api/users/{id}} (USR-04).
 *
 * <p>{@code password} y {@code confirmPassword} son opcionales: si
 * {@code password} se omite o se envía vacío, la contraseña actual se
 * preserva. La regla de tamaño mínimo se evalúa con
 * {@code @Size(min = 8)}, que por defecto ignora los valores {@code null};
 * la cadena vacía no supera {@code min=8}, por lo que el servicio debería
 * tratarla como "mantener" antes de llegar aquí, o bien el cliente debe
 * omitir el campo. {@link PasswordsMatch} solo entra en acción cuando
 * {@code password} es no nulo y no vacío.
 */
@PasswordsMatch
public record UpdateUserRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String fullName,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 150, message = "El correo electrónico no puede exceder 150 caracteres")
        String email,

        @NotNull(message = "El rol es obligatorio")
        UserRole role,

        @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
        String password,

        String confirmPassword
) {
}
