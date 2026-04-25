package co.edu.docurural.web.dto.user;

import co.edu.docurural.domain.enums.enums.UserRole;
import co.edu.docurural.web.dto.validation.PasswordsMatch;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Datos para crear un nuevo usuario")
@PasswordsMatch
public record CreateUserRequest(

        @NotBlank(message = "{validation.user.full-name.required}")
        @Size(min = 3, max = 100, message = "{validation.user.full-name.size}")
        @Schema(description = "Nombre completo", example = "María García López")
        String fullName,

        @NotBlank(message = "{validation.user.email.required}")
        @Email(message = "{validation.user.email.format}")
        @Size(max = 150, message = "{validation.user.email.size}")
        @Schema(description = "Email único en el sistema", example = "maria.garcia@docurural.edu.co")
        String email,

        @NotBlank(message = "{validation.user.password.required}")
        @Size(min = 8, message = "{validation.user.password.size}")
        @Schema(description = "Contraseña (mín. 8 caracteres)", example = "Segura123!")
        String password,

        @NotBlank(message = "{validation.user.confirm-password.required}")
        @Schema(description = "Confirmación de contraseña", example = "Segura123!")
        String confirmPassword,

        @NotNull(message = "{validation.user.role.required}")
        @Schema(description = "Rol asignado: ADMIN | EDITOR | READER", example = "EDITOR")
        UserRole role
) {
}
