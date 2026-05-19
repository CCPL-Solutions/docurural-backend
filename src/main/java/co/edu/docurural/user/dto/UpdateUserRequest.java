package co.edu.docurural.user.dto;

import co.edu.docurural.shared.domain.enums.UserRole;
import co.edu.docurural.user.dto.validation.PasswordsMatch;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Campos editables del usuario")
public record UpdateUserRequest(

        @NotBlank(message = "{validation.user.full-name.required}")
        @Size(min = 3, max = 100, message = "{validation.user.full-name.size}")
        @Schema(description = "Nombre completo", example = "María García López")
        String fullName,

        @NotBlank(message = "{validation.user.email.required}")
        @Email(message = "{validation.user.email.format}")
        @Size(max = 150, message = "{validation.user.email.size}")
        @Schema(description = "Email", example = "maria.garcia@docurural.edu.co")
        String email,

        @NotNull(message = "{validation.user.role.required}")
        @Schema(description = "Nuevo rol", example = "READER")
        UserRole role,

        @Size(min = 12, max = 128, message = "{validation.user.password.size}")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
                message = "{validation.user.password.complexity}")
        @Schema(description = "Nueva contraseña opcional (mín. 12 caracteres, mayúscula, minúscula, dígito y símbolo)", example = "NuevaClave1!")
        String password,

        @Schema(description = "Confirmación de la nueva contraseña (debe coincidir con 'password')", example = "NuevaClave1!")
        String confirmPassword
) {
}
