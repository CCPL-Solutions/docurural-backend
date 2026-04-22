package co.edu.docurural.web.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Representacion publica de un usuario.
 *
 * <p>Usado por USR-01 (items del listado) y USR-02 (detalle). Nunca incluye
 * {@code passwordHash}. {@code lastLogin} es nullable cuando el usuario nunca
 * ha ingresado; se serializa como {@code null} explicito.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime lastLogin
) {
}
