package co.edu.docurural.support;

import co.edu.docurural.auth.dto.LoginRequest;
import co.edu.docurural.category.dto.CreateCategoryRequest;
import co.edu.docurural.category.dto.UpdateCategoryRequest;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequest;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.document.dto.UpdateDocumentMetadataRequest;
import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.shared.domain.entity.User;
import co.edu.docurural.shared.domain.enums.UserRole;
import co.edu.docurural.shared.domain.enums.UserStatus;
import co.edu.docurural.user.dto.CreateUserRequest;
import co.edu.docurural.user.dto.UpdateStatusRequest;
import co.edu.docurural.user.dto.UpdateUserRequest;

import java.time.LocalDate;
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

    public static Category categoryActive(Long id, String name, String description, User createdBy) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .status(CategoryStatus.ACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .createdBy(createdBy)
                .build();
    }

    public static CreateCategoryRequest createCategoryRequest(String name, String description) {
        return new CreateCategoryRequest(name, description);
    }

    public static UpdateCategoryRequest updateCategoryRequest(String name, String description) {
        return new UpdateCategoryRequest(name, description);
    }

    public static Category categoryInactive(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .status(CategoryStatus.INACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static UpdateCategoryStatusRequest updateCategoryStatusRequest(CategoryStatus status) {
        return new UpdateCategoryStatusRequest(status);
    }

    public static UploadDocumentRequest uploadDocumentRequest(Long categoryId) {
        return new UploadDocumentRequest(
                "Acta Consejo Directivo Marzo 2026",
                categoryId,
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "Acta de reunión");
    }

    public static UpdateDocumentMetadataRequest updateDocumentMetadataRequest(Long categoryId) {
        return new UpdateDocumentMetadataRequest(
                "Acta Consejo Directivo Marzo 2026 - Revisado",
                categoryId,
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "Versión corregida del acta");
    }

    public static Document documentActive(Long id, Category category, User uploadedBy) {
        return Document.builder()
                .id(id)
                .title("Acta Consejo Directivo Marzo 2026")
                .description("Acta de reunión")
                .category(category)
                .responsibleArea("Rectoría")
                .documentDate(LocalDate.of(2026, 3, 15))
                .filePath("/uploads/documents/2026/05/uuid.pdf")
                .originalFileName("acta.pdf")
                .fileFormat(DocumentFormat.PDF)
                .fileSizeBytes(524288L)
                .uploadedBy(uploadedBy)
                .status(DocumentStatus.ACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }
}
