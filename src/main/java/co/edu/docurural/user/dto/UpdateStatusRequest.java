package co.edu.docurural.user.dto;

import co.edu.docurural.shared.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request body de {@code PATCH /api/users/{id}/status} (USR-05).
 *
 * <p>Se usa el enum {@link UserStatus} en lugar de {@code @Pattern} para que
 * valores inválidos sean rechazados por Jackson antes de llegar al controller,
 * y para mantener la consistencia con el resto del dominio.
 */
@Schema(description = "Nuevo estado del usuario")
public record UpdateStatusRequest(

        @NotNull(message = "{validation.user.status.required}")
        @Schema(description = "Estado: ACTIVE | INACTIVE", example = "INACTIVE")
        UserStatus status
) {
}
