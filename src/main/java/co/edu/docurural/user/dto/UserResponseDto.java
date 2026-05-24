package co.edu.docurural.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Representación pública de un usuario.
 *
 * <p>Usado por USR-01 (items del listado) y USR-02 (detalle). Nunca incluye
 * {@code passwordHash}. {@code lastLogin} es nullable cuando el usuario nunca
 * ha ingresado; se serializa como {@code null} explícito.
 */
@Schema(description = "Detalle completo de un usuario")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record UserResponseDto(
        @Schema(description = "ID del usuario", example = "5")
        Long id,
        @Schema(description = "Nombre completo", example = "María García López")
        String fullName,
        @Schema(description = "Email único en el sistema", example = "maria.garcia@docurural.edu.co")
        String email,
        @Schema(description = "Rol asignado: ADMIN | EDITOR | READER", example = "READER")
        String role,
        @Schema(description = "Estado del usuario", example = "ACTIVE")
        String status,
        @Schema(description = "Fecha y hora de creación del usuario", example = "2024-06-01T12:34:56")
        LocalDateTime createdAt,
        @Schema(description = "Null si nunca ha iniciado sesión", example = "2026-04-24T10:15:30", nullable = true)
        LocalDateTime lastLogin
) {
}
