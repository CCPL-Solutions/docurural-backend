package co.edu.docurural.user.mapper;

import co.edu.docurural.user.domain.entity.User;
import co.edu.docurural.auth.dto.UserSummary;
import co.edu.docurural.user.dto.CreateUserResponse;
import co.edu.docurural.user.dto.UpdateStatusResponse;
import co.edu.docurural.user.dto.UpdateUserResponse;
import co.edu.docurural.user.dto.UserListResponse;
import co.edu.docurural.user.dto.UserResponse;

import java.util.List;
import java.util.Objects;

/**
 * Mapper estático sin dependencias (sin MapStruct) entre la entidad
 * {@link User} y los DTOs públicos.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Asegurar que {@code passwordHash} nunca se exponga.</li>
 *   <li>Convertir enums a {@link String} para el contrato JSON.</li>
 *   <li>Centralizar los mensajes de confirmación reutilizados por el servicio.</li>
 * </ul>
 */
public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Convierte una entidad {@link User} a su representación pública.
     */
    public static UserResponse toResponse(User user) {
        Objects.requireNonNull(user, "user no puede ser null");
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }

    /**
     * Convierte la entidad al resumen utilizado dentro de {@code LoginResponse}.
     */
    public static UserSummary toSummary(User user) {
        Objects.requireNonNull(user, "user no puede ser null");
        return new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null
        );
    }

    /**
     * Empaqueta la lista completa con el total de usuarios.
     */
    public static UserListResponse toListResponse(List<User> users) {
        Objects.requireNonNull(users, "users no puede ser null");
        List<UserResponse> items = users.stream()
                .map(UserMapper::toResponse)
                .toList();
        return new UserListResponse(items.size(), items);
    }

    /**
     * Construye la respuesta de creación de usuario (USR-03).
     */
    public static CreateUserResponse toCreateResponse(User user, String message) {
        Objects.requireNonNull(user, "user no puede ser null");
        return new CreateUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getCreatedAt(),
                message
        );
    }

    /**
     * Construye la respuesta de edición de usuario (USR-04).
     */
    public static UpdateUserResponse toUpdateResponse(User user, String message) {
        Objects.requireNonNull(user, "user no puede ser null");
        return new UpdateUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                message
        );
    }

    /**
     * Construye la respuesta de cambio de estado (USR-05).
     */
    public static UpdateStatusResponse toStatusResponse(User user, String message) {
        Objects.requireNonNull(user, "user no puede ser null");
        return new UpdateStatusResponse(
                user.getId(),
                user.getFullName(),
                user.getStatus() != null ? user.getStatus().name() : null,
                message
        );
    }
}
