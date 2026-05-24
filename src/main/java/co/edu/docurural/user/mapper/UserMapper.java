package co.edu.docurural.user.mapper;

import co.edu.docurural.auth.dto.UserSummary;
import co.edu.docurural.user.dto.CreateUserResponse;
import co.edu.docurural.user.dto.UpdateStatusResponse;
import co.edu.docurural.user.dto.UpdateUserResponse;
import co.edu.docurural.user.dto.UserListResponse;
import co.edu.docurural.user.dto.UserResponse;
import co.edu.docurural.user.entity.User;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @BeforeMapping
    protected void requireNonNull(User user) {
        Objects.requireNonNull(user, "user no puede ser null");
    }

    @Mapping(target = "role",
            expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "status",
            expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    public abstract UserResponse toResponse(User user);

    @Mapping(target = "role",
            expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    public abstract UserSummary toSummary(User user);

    @Mapping(target = "role",
            expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "status",
            expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    public abstract CreateUserResponse toCreateResponse(User user, String message);

    @Mapping(target = "role",
            expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "status",
            expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    public abstract UpdateUserResponse toUpdateResponse(User user, String message);

    @Mapping(target = "status",
            expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    public abstract UpdateStatusResponse toStatusResponse(User user, String message);

    public UserListResponse toListResponse(List<User> users) {
        Objects.requireNonNull(users, "users no puede ser null");
        List<UserResponse> items = users.stream()
                .map(this::toResponse)
                .toList();
        return new UserListResponse(items.size(), items);
    }
}
