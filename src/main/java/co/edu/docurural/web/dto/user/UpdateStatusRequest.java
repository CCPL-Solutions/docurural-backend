package co.edu.docurural.web.dto.user;

import co.edu.docurural.domain.enums.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body de {@code PATCH /api/users/{id}/status} (USR-05).
 *
 * <p>Se usa el enum {@link UserStatus} en lugar de {@code @Pattern} para que
 * valores inválidos sean rechazados por Jackson antes de llegar al controller,
 * y para mantener la consistencia con el resto del dominio.
 */
public record UpdateStatusRequest(

        @NotNull(message = "{validation.user.status.required}")
        UserStatus status
) {
}
