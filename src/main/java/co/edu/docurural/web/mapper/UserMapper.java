package co.edu.docurural.web.mapper;

import co.edu.docurural.domain.entity.User;
import co.edu.docurural.web.dto.auth.UserSummary;
import co.edu.docurural.web.dto.user.CreateUserResponse;
import co.edu.docurural.web.dto.user.UpdateStatusResponse;
import co.edu.docurural.web.dto.user.UpdateUserResponse;
import co.edu.docurural.web.dto.user.UserListResponse;
import co.edu.docurural.web.dto.user.UserResponse;

import java.util.List;
import java.util.Objects;

/**
 * Mapper estatico sin dependencias (sin MapStruct) entre la entidad
 * {@link User} y los DTOs publicos.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Asegurar que {@code passwordHash} nunca se exponga.</li>
 *   <li>Convertir enums a {@link String} para el contrato JSON.</li>
 *   <li>Centralizar los mensajes de confirmacion reutilizados por el servicio.</li>
 * </ul>
 */
public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Convierte una entidad {@link User} a su representacion publica.
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
     * Construye la respuesta de creacion de usuario (USR-03).
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
     * Construye la respuesta de edicion de usuario (USR-04).
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
