package co.edu.docurural.support;

import co.edu.docurural.auth.dto.LoginRequest;
import co.edu.docurural.category.dto.CreateCategoryRequest;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.shared.domain.entity.User;
import co.edu.docurural.shared.domain.enums.UserRole;
import co.edu.docurural.shared.domain.enums.UserStatus;
import co.edu.docurural.user.dto.CreateUserRequest;
import co.edu.docurural.user.dto.UpdateStatusRequest;
import co.edu.docurural.user.dto.UpdateUserRequest;

import java.time.LocalDateTime;

public final class TestFixtures {

    public static final LocalDateTime FIXED_CREATED_AT = LocalDateTime.of(2026, 1, 1, 8, 0, 0);

    private TestFixtures() {
    }

    public static User userAdmin(Long id) {
        return User.builder()
                .id(id)
                .fullName("Ana Admin")
                .email("ana.admin@docurural.edu.co")
                .passwordHash("$2a$10$hashedAdminPwd")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static User userEditor(Long id) {
        return User.builder()
                .id(id)
                .fullName("Erik Editor")
                .email("erik.editor@docurural.edu.co")
                .passwordHash("$2a$10$hashedEditorPwd")
                .role(UserRole.EDITOR)
                .status(UserStatus.ACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static User userInactive(Long id) {
        return User.builder()
                .id(id)
                .fullName("Ida Inactive")
                .email("ida.inactive@docurural.edu.co")
                .passwordHash("$2a$10$hashedInactivePwd")
                .role(UserRole.READER)
                .status(UserStatus.INACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static LoginRequest loginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }

    public static CreateUserRequest createUserRequest(
            String fullName, String email, String password, UserRole role) {
        return new CreateUserRequest(fullName, email, password, password, role);
    }

    public static CreateUserRequest createUserRequest(
            String fullName, String email, String password, String confirmPassword, UserRole role) {
        return new CreateUserRequest(fullName, email, password, confirmPassword, role);
    }

    public static UpdateUserRequest updateUserRequest(
            String fullName, String email, UserRole role) {
        return new UpdateUserRequest(fullName, email, role, null, null);
    }

    public static UpdateUserRequest updateUserRequest(
            String fullName, String email, UserRole role, String password, String confirmPassword) {
        return new UpdateUserRequest(fullName, email, role, password, confirmPassword);
    }

    public static UpdateStatusRequest updateStatusRequest(UserStatus status) {
        return new UpdateStatusRequest(status);
    }

    public static Category categoryActive(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(null)
                .status(CategoryStatus.ACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static Category categoryActive(Long id, String name, String description) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .status(CategoryStatus.ACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static CreateCategoryRequest createCategoryRequest(String name, String description) {
        return new CreateCategoryRequest(name, description);
    }
}
