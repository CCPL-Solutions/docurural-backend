package co.edu.docurural.user.mapper;

import co.edu.docurural.auth.dto.UserSummaryDto;
import co.edu.docurural.user.dto.CreateUserResponseDto;
import co.edu.docurural.user.dto.UpdateStatusResponseDto;
import co.edu.docurural.user.dto.UpdateUserResponseDto;
import co.edu.docurural.user.dto.UserListResponseDto;
import co.edu.docurural.user.dto.UserResponseDto;
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
    public abstract UserResponseDto toResponse(User user);

    @Mapping(target = "role",
            expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    public abstract UserSummaryDto toSummary(User user);

    @Mapping(target = "role",
            expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "status",
            expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    public abstract CreateUserResponseDto toCreateResponse(User user, String message);

    @Mapping(target = "role",
            expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "status",
            expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    public abstract UpdateUserResponseDto toUpdateResponse(User user, String message);

    @Mapping(target = "status",
            expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    public abstract UpdateStatusResponseDto toStatusResponse(User user, String message);

    public UserListResponseDto toListResponse(List<User> users) {
        Objects.requireNonNull(users, "users no puede ser null");
        List<UserResponseDto> items = users.stream()
                .map(this::toResponse)
                .toList();
        return new UserListResponseDto(items.size(), items);
    }
}
