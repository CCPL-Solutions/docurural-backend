package co.edu.docurural.web.dto.user;

import co.edu.docurural.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body de {@code PATCH /api/users/{id}/status} (USR-05).
 *
 * <p>Se usa el enum {@link UserStatus} en lugar de {@code @Pattern} para que
 * valores invalidos sean rechazados por Jackson antes de llegar al controller,
 * y para mantener la consistencia con el resto del dominio.
 */
public record UpdateStatusRequest(

        @NotNull(message = "El estado es obligatorio")
        UserStatus status
) {
}
