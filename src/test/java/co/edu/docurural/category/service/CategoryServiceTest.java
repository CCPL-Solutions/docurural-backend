package co.edu.docurural.category.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.dto.CreateCategoryRequest;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryRequest;
import co.edu.docurural.category.dto.UpdateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequest;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponse;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final AuditContext AUDIT_ADMIN = new AuditContext(ADMIN_ID, "203.0.113.10");

    @Mock
    CategoryRepository categoryRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ActivityLogService activityLogService;
    @Mock
    MessageResolver messageResolver;

    @InjectMocks
    CategoryService categoryService;

    @BeforeEach
    void stubMessageResolver() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    void create_persistsAndLogs_whenNameIsUnique() {
        CreateCategoryRequest request = TestFixtures.createCategoryRequest(
                "Proyectos Biotecnología",
                "Proyectos e informes del laboratorio de biotecnología");

        when(categoryRepository.existsByName(request.name())).thenReturn(false);
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(TestFixtures.userAdmin(ADMIN_ID));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(9L);
            c.setStatus(CategoryStatus.ACTIVE);
            c.setCreatedAt(TestFixtures.FIXED_CREATED_AT);
            return c;
        });

        CreateCategoryResponse response = categoryService.create(request, AUDIT_ADMIN);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.name()).isEqualTo("Proyectos Biotecnología");
        assertThat(response.description()).isEqualTo("Proyectos e informes del laboratorio de biotecnología");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("category.created.success");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        Category persisted = captor.getValue();
        assertThat(persisted.getName()).isEqualTo("Proyectos Biotecnología");
        assertThat(persisted.getDescription()).isEqualTo("Proyectos e informes del laboratorio de biotecnología");
        assertThat(persisted.getCreatedBy()).isNotNull();

        verify(activityLogService).record(
                eq(ActivityAction.CREATE_CATEGORY),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Categoria creada: Proyectos Biotecnología"));
    }

    @Test
    void create_withNullDescription_persistsCategoryWithNullDescription() {
        CreateCategoryRequest request = TestFixtures.createCategoryRequest("Circulares", null);

        when(categoryRepository.existsByName(request.name())).thenReturn(false);
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(TestFixtures.userAdmin(ADMIN_ID));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            c.setStatus(CategoryStatus.ACTIVE);
            c.setCreatedAt(TestFixtures.FIXED_CREATED_AT);
            return c;
        });

        CreateCategoryResponse response = categoryService.create(request, AUDIT_ADMIN);

        assertThat(response.description()).isNull();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    void create_throwsConflict_whenNameAlreadyExists() {
        CreateCategoryRequest request = TestFixtures.createCategoryRequest("Actas", null);
        when(categoryRepository.existsByName("Actas")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request, AUDIT_ADMIN))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void create_throwsIllegalArgument_whenAuditIsNull() {
        CreateCategoryRequest request = TestFixtures.createCategoryRequest("Actas", null);

        assertThatThrownBy(() -> categoryService.create(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit no puede ser null");

        verifyNoInteractions(categoryRepository, activityLogService);
    }

    @Test
    void create_throwsIllegalArgument_whenActorUserIdIsNull() {
        CreateCategoryRequest request = TestFixtures.createCategoryRequest("Actas", null);
        AuditContext auditWithNullActor = new AuditContext(null, "127.0.0.1");

        assertThatThrownBy(() -> categoryService.create(request, auditWithNullActor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit.actorUserId no puede ser null");

        verifyNoInteractions(categoryRepository, activityLogService);
    }

    // ------------------------------------------------------------------
    // update()
    // ------------------------------------------------------------------

    @Test
    void update_persistsAndLogs_whenNameAndDescriptionChange() {
        Long categoryId = 9L;
        Category existing = TestFixtures.categoryActive(categoryId, "Proyectos Biotecnología", "Descripción anterior");
        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest(
                "Proyectos e Informes Biotecnología",
                "Descripción nueva");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot(request.name(), categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryResponse response = categoryService.update(categoryId, request, AUDIT_ADMIN);

        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo("Proyectos e Informes Biotecnología");
        assertThat(response.description()).isEqualTo("Descripción nueva");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("category.updated.success");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Proyectos e Informes Biotecnología");
        assertThat(captor.getValue().getDescription()).isEqualTo("Descripción nueva");

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_CATEGORY),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: [name, description]"));
    }

    @Test
    void update_onlyName_logsModifiedFieldsName() {
        Long categoryId = 9L;
        Category existing = TestFixtures.categoryActive(categoryId, "Proyectos Biotecnología", "Descripción actual");
        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest(
                "Proyectos e Informes Biotecnología",
                "Descripción actual");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot(request.name(), categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.update(categoryId, request, AUDIT_ADMIN);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_CATEGORY),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: [name]"));
    }

    @Test
    void update_descriptionNull_preservesCurrentDescription() {
        Long categoryId = 9L;
        Category existing = TestFixtures.categoryActive(categoryId, "Actas", "Descripción actual");
        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest("Actas Actualizadas", null);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot(request.name(), categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryResponse response = categoryService.update(categoryId, request, AUDIT_ADMIN);

        assertThat(response.description()).isEqualTo("Descripción actual");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Descripción actual");

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_CATEGORY),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: [name]"));
    }

    @Test
    void update_nameUnchanged_skipsUniquenessCheck() {
        Long categoryId = 9L;
        Category existing = TestFixtures.categoryActive(categoryId, "Actas", "Descripción");
        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest("Actas", "Nueva descripción");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.update(categoryId, request, AUDIT_ADMIN);

        verify(categoryRepository, never()).existsByNameAndIdNot(anyString(), anyLong());
    }

    @Test
    void update_throwsNotFound_whenIdMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L,
                TestFixtures.updateCategoryRequest("Nombre", null), AUDIT_ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void update_throwsForbidden_whenCategoryInactive() {
        Long categoryId = 5L;
        Category inactive = TestFixtures.categoryInactive(categoryId, "Actas");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> categoryService.update(categoryId,
                TestFixtures.updateCategoryRequest("Actas Nuevo", null), AUDIT_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessErrorCode.FORBIDDEN);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void update_throwsConflict_whenNewNameUsedByAnother() {
        Long categoryId = 9L;
        Category existing = TestFixtures.categoryActive(categoryId, "Proyectos Biotecnología", null);
        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest("Actas", null);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot("Actas", categoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(categoryId, request, AUDIT_ADMIN))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void update_throwsIllegalArgument_whenAuditIsNull() {
        assertThatThrownBy(() -> categoryService.update(1L,
                TestFixtures.updateCategoryRequest("Nombre", null), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit no puede ser null");

        verifyNoInteractions(categoryRepository, activityLogService);
    }

    @Test
    void update_throwsIllegalArgument_whenActorUserIdIsNull() {
        AuditContext auditWithNullActor = new AuditContext(null, "127.0.0.1");

        assertThatThrownBy(() -> categoryService.update(1L,
                TestFixtures.updateCategoryRequest("Nombre", null), auditWithNullActor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit.actorUserId no puede ser null");

        verifyNoInteractions(categoryRepository, activityLogService);
    }

    // ------------------------------------------------------------------
    // changeStatus()
    // ------------------------------------------------------------------

    @Test
    void changeStatus_deactivates_persistsAndLogs() {
        Long categoryId = 9L;
        Category existing = TestFixtures.categoryActive(categoryId, "Proyectos e Informes Biotecnología");
        UpdateCategoryStatusRequest request = TestFixtures.updateCategoryStatusRequest(CategoryStatus.INACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryStatusResponse response = categoryService.changeStatus(categoryId, request, AUDIT_ADMIN);

        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo("Proyectos e Informes Biotecnología");
        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(response.message()).isEqualTo("category.deactivated.success");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CategoryStatus.INACTIVE);

        verify(activityLogService).record(
                eq(ActivityAction.DEACTIVATE_CATEGORY),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Estado cambiado a INACTIVE"));
    }

    @Test
    void changeStatus_reactivates_persistsAndLogs() {
        Long categoryId = 5L;
        Category existing = TestFixtures.categoryInactive(categoryId, "Actas");
        UpdateCategoryStatusRequest request = TestFixtures.updateCategoryStatusRequest(CategoryStatus.ACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryStatusResponse response = categoryService.changeStatus(categoryId, request, AUDIT_ADMIN);

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("category.activated.success");

        verify(activityLogService).record(
                eq(ActivityAction.DEACTIVATE_CATEGORY),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Estado cambiado a ACTIVE"));
    }

    @Test
    void changeStatus_throwsNotFound_whenIdMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.changeStatus(99L,
                TestFixtures.updateCategoryStatusRequest(CategoryStatus.INACTIVE), AUDIT_ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void changeStatus_throwsBadRequest_whenAlreadyInRequestedStatus() {
        Long categoryId = 9L;
        Category existing = TestFixtures.categoryActive(categoryId, "Actas");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> categoryService.changeStatus(categoryId,
                TestFixtures.updateCategoryStatusRequest(CategoryStatus.ACTIVE), AUDIT_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void changeStatus_throwsIllegalArgument_whenAuditIsNull() {
        assertThatThrownBy(() -> categoryService.changeStatus(1L,
                TestFixtures.updateCategoryStatusRequest(CategoryStatus.INACTIVE), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit no puede ser null");

        verifyNoInteractions(categoryRepository, activityLogService);
    }

    @Test
    void changeStatus_throwsIllegalArgument_whenActorUserIdIsNull() {
        AuditContext auditWithNullActor = new AuditContext(null, "127.0.0.1");

        assertThatThrownBy(() -> categoryService.changeStatus(1L,
                TestFixtures.updateCategoryStatusRequest(CategoryStatus.INACTIVE), auditWithNullActor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit.actorUserId no puede ser null");

        verifyNoInteractions(categoryRepository, activityLogService);
    }
}
