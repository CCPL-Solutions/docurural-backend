package co.edu.docurural.support;

import co.edu.docurural.auth.dto.LoginRequestDto;
import co.edu.docurural.category.dto.CreateCategoryRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequestDto;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.document.dto.UpdateDocumentMetadataRequestDto;
import co.edu.docurural.document.dto.UploadDocumentRequestDto;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.shared.enums.SensitivityLevel;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.enums.UserRole;
import co.edu.docurural.user.enums.UserStatus;
import co.edu.docurural.user.dto.CreateUserRequestDto;
import co.edu.docurural.user.dto.UpdateStatusRequestDto;
import co.edu.docurural.user.dto.UpdateUserRequestDto;

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

    public static LoginRequestDto loginRequest(String email, String password) {
        return new LoginRequestDto(email, password);
    }

    public static CreateUserRequestDto createUserRequest(
            String fullName, String email, String password, UserRole role) {
        return new CreateUserRequestDto(fullName, email, password, password, role);
    }

    public static CreateUserRequestDto createUserRequest(
            String fullName, String email, String password, String confirmPassword, UserRole role) {
        return new CreateUserRequestDto(fullName, email, password, confirmPassword, role);
    }

    public static UpdateUserRequestDto updateUserRequest(
            String fullName, String email, UserRole role) {
        return new UpdateUserRequestDto(fullName, email, role, null, null);
    }

    public static UpdateUserRequestDto updateUserRequest(
            String fullName, String email, UserRole role, String password, String confirmPassword) {
        return new UpdateUserRequestDto(fullName, email, role, password, confirmPassword);
    }

    public static UpdateStatusRequestDto updateStatusRequest(UserStatus status) {
        return new UpdateStatusRequestDto(status);
    }

    public static Category categoryActive(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(null)
                .status(CategoryStatus.ACTIVE)
                .defaultSensitivityLevel(SensitivityLevel.INTERNAL)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static Category categoryActive(Long id, String name, SensitivityLevel defaultSensitivityLevel) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(null)
                .status(CategoryStatus.ACTIVE)
                .defaultSensitivityLevel(defaultSensitivityLevel)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static Category categoryActive(Long id, String name, String description) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .status(CategoryStatus.ACTIVE)
                .defaultSensitivityLevel(SensitivityLevel.INTERNAL)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static Category categoryActive(Long id, String name, String description, User createdBy) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .status(CategoryStatus.ACTIVE)
                .defaultSensitivityLevel(SensitivityLevel.INTERNAL)
                .createdAt(FIXED_CREATED_AT)
                .createdBy(createdBy)
                .build();
    }

    public static CreateCategoryRequestDto createCategoryRequest(String name, String description) {
        return new CreateCategoryRequestDto(name, description, SensitivityLevel.INTERNAL);
    }

    public static CreateCategoryRequestDto createCategoryRequest(String name, String description,
                                                                  SensitivityLevel defaultSensitivityLevel) {
        return new CreateCategoryRequestDto(name, description, defaultSensitivityLevel);
    }

    public static UpdateCategoryRequestDto updateCategoryRequest(String name, String description) {
        return new UpdateCategoryRequestDto(name, description, SensitivityLevel.INTERNAL);
    }

    public static UpdateCategoryRequestDto updateCategoryRequest(String name, String description,
                                                                  SensitivityLevel defaultSensitivityLevel) {
        return new UpdateCategoryRequestDto(name, description, defaultSensitivityLevel);
    }

    public static Category categoryInactive(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .status(CategoryStatus.INACTIVE)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static UpdateCategoryStatusRequestDto updateCategoryStatusRequest(CategoryStatus status) {
        return new UpdateCategoryStatusRequestDto(status);
    }

    public static UploadDocumentRequestDto uploadDocumentRequest(Long categoryId) {
        return new UploadDocumentRequestDto(
                "Acta Consejo Directivo Marzo 2026",
                categoryId,
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "Acta de reunión",
                SensitivityLevel.INTERNAL);
    }

    public static UploadDocumentRequestDto uploadDocumentRequest(Long categoryId, SensitivityLevel sensitivityLevel) {
        return new UploadDocumentRequestDto(
                "Acta Consejo Directivo Marzo 2026",
                categoryId,
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "Acta de reunión",
                sensitivityLevel);
    }

    public static UpdateDocumentMetadataRequestDto updateDocumentMetadataRequest(Long categoryId) {
        return new UpdateDocumentMetadataRequestDto(
                "Acta Consejo Directivo Marzo 2026 - Revisado",
                categoryId,
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "Versión corregida del acta",
                SensitivityLevel.INTERNAL);
    }

    public static UpdateDocumentMetadataRequestDto updateDocumentMetadataRequest(Long categoryId,
                                                                                  SensitivityLevel sensitivityLevel) {
        return new UpdateDocumentMetadataRequestDto(
                "Acta Consejo Directivo Marzo 2026 - Revisado",
                categoryId,
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "Versión corregida del acta",
                sensitivityLevel);
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
                .sensitivityLevel(SensitivityLevel.INTERNAL)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }

    public static Document documentActive(Long id, Category category, User uploadedBy,
                                          SensitivityLevel sensitivityLevel) {
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
                .sensitivityLevel(sensitivityLevel)
                .createdAt(FIXED_CREATED_AT)
                .build();
    }
}
