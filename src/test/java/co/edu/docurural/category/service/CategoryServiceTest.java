package co.edu.docurural.category.service;

import co.edu.docurural.category.dto.CategoryDetailResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponseDto;
import co.edu.docurural.category.mapper.CategoryMapper;
import co.edu.docurural.activitylog.enums.ActivityAction;
import org.mapstruct.factory.Mappers;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.dto.CategoryListResponseDto;
import co.edu.docurural.category.dto.CreateCategoryRequestDto;
import co.edu.docurural.category.dto.CreateCategoryResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequestDto;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.category.repository.projection.CategoryCountView;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.shared.util.SortingValidator;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
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

    CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageResolver.get(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        categoryService = new CategoryServiceImpl(categoryRepository, userRepository,
                activityLogService, messageResolver, new SortingValidator(messageResolver),
                Mappers.getMapper(CategoryMapper.class));
    }

    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    void create_persistsAndLogs_whenNameIsUnique() {
        CreateCategoryRequestDto request = TestFixtures.createCategoryRequest(
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

        CreateCategoryResponseDto response = categoryService.create(request, AUDIT_ADMIN);

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
        CreateCategoryRequestDto request = TestFixtures.createCategoryRequest("Circulares", null);

        when(categoryRepository.existsByName(request.name())).thenReturn(false);
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(TestFixtures.userAdmin(ADMIN_ID));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            c.setStatus(CategoryStatus.ACTIVE);
            c.setCreatedAt(TestFixtures.FIXED_CREATED_AT);
            return c;
        });

        CreateCategoryResponseDto response = categoryService.create(request, AUDIT_ADMIN);

        assertThat(response.description()).isNull();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    void create_throwsConflict_whenNameAlreadyExists() {
        CreateCategoryRequestDto request = TestFixtures.createCategoryRequest("Actas", null);
        when(categoryRepository.existsByName("Actas")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request, AUDIT_ADMIN))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void create_throwsIllegalArgument_whenAuditIsNull() {
        CreateCategoryRequestDto request = TestFixtures.createCategoryRequest("Actas", null);

        assertThatThrownBy(() -> categoryService.create(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit no puede ser null");

        verifyNoInteractions(categoryRepository, activityLogService);
    }

    @Test
    void create_throwsIllegalArgument_whenActorUserIdIsNull() {
        CreateCategoryRequestDto request = TestFixtures.createCategoryRequest("Actas", null);
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
        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest(
                "Proyectos e Informes Biotecnología",
                "Descripción nueva");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot(request.name(), categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryResponseDto response = categoryService.update(categoryId, request, AUDIT_ADMIN);

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
        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest(
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
        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest("Actas Actualizadas", null);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot(request.name(), categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryResponseDto response = categoryService.update(categoryId, request, AUDIT_ADMIN);

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
        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest("Actas", "Nueva descripción");

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
        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest("Actas", null);

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
        UpdateCategoryStatusRequestDto request = TestFixtures.updateCategoryStatusRequest(CategoryStatus.INACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryStatusResponseDto response = categoryService.changeStatus(categoryId, request, AUDIT_ADMIN);

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
        UpdateCategoryStatusRequestDto request = TestFixtures.updateCategoryStatusRequest(CategoryStatus.ACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryStatusResponseDto response = categoryService.changeStatus(categoryId, request, AUDIT_ADMIN);

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("category.activated.success");

        verify(activityLogService).record(
                eq(ActivityAction.ACTIVATE_CATEGORY),
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

    // ------------------------------------------------------------------
    // list()
    // ------------------------------------------------------------------

    @Test
    void list_returnsAllCategoriesWithCounts_andSummary() {
        Category cat1 = TestFixtures.categoryActive(1L, "Actas", "Actas de reuniones");
        Category cat2 = TestFixtures.categoryActive(3L, "Informes", null);
        Category cat3 = TestFixtures.categoryInactive(5L, "Resoluciones");

        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of(cat1, cat2, cat3));
        when(categoryRepository.countActiveDocumentsByCategory())
                .thenReturn(List.of(countView(1L, 23L), countView(5L, 7L)));

        CategoryListResponseDto response = categoryService.list(null, null);

        assertThat(response.totalCategories()).isEqualTo(3);
        assertThat(response.activeCategories()).isEqualTo(2);
        assertThat(response.inactiveCategories()).isEqualTo(1);
        assertThat(response.categories()).hasSize(3);
        assertThat(response.categories().get(0).documentCount()).isEqualTo(23);
        assertThat(response.categories().get(1).documentCount()).isEqualTo(0);
        assertThat(response.categories().get(2).documentCount()).isEqualTo(7);
    }

    @Test
    void list_withDefaultParams_sortsByNameAsc() {
        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(categoryRepository.countActiveDocumentsByCategory()).thenReturn(List.of());

        categoryService.list(null, null);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(categoryRepository).findAll(sortCaptor.capture());
        Sort.Order order = sortCaptor.getValue().getOrderFor("name");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_withCreatedAtDesc_passesSortToRepo() {
        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(categoryRepository.countActiveDocumentsByCategory()).thenReturn(List.of());

        categoryService.list("createdAt", "desc");

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(categoryRepository).findAll(sortCaptor.capture());
        Sort.Order order = sortCaptor.getValue().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void list_withInvalidSortBy_throwsBusinessRule() {
        assertThatThrownBy(() -> categoryService.list("invalid", null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT);

        verifyNoInteractions(categoryRepository, activityLogService);
    }

    @Test
    void list_withInvalidSortDir_throwsBusinessRule() {
        assertThatThrownBy(() -> categoryService.list("name", "sideways"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT);

        verify(categoryRepository, never()).findAll(any(Sort.class));
    }

    @Test
    void list_returnsEmptyResponse_whenNoCategories() {
        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(categoryRepository.countActiveDocumentsByCategory()).thenReturn(List.of());

        CategoryListResponseDto response = categoryService.list(null, null);

        assertThat(response.totalCategories()).isEqualTo(0);
        assertThat(response.activeCategories()).isEqualTo(0);
        assertThat(response.inactiveCategories()).isEqualTo(0);
        assertThat(response.categories()).isEmpty();
    }

    @Test
    void list_categoriesWithoutActiveDocuments_haveCountZero() {
        when(categoryRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(TestFixtures.categoryActive(1L, "Actas")));
        when(categoryRepository.countActiveDocumentsByCategory()).thenReturn(List.of());

        CategoryListResponseDto response = categoryService.list(null, null);

        assertThat(response.categories().get(0).documentCount()).isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // findById()
    // ------------------------------------------------------------------

    @Test
    void findById_returnsDetail_withDocumentCount() {
        Category category = TestFixtures.categoryActive(1L, "Actas", "Actas de reuniones");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.countActiveDocumentsByCategoryId(1L)).thenReturn(15L);

        CategoryDetailResponseDto response = categoryService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Actas");
        assertThat(response.description()).isEqualTo("Actas de reuniones");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.documentCount()).isEqualTo(15);
    }

    @Test
    void findById_returnsZeroCount_whenCategoryHasNoActiveDocuments() {
        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(TestFixtures.categoryActive(2L, "Resoluciones")));
        when(categoryRepository.countActiveDocumentsByCategoryId(2L)).thenReturn(0L);

        CategoryDetailResponseDto response = categoryService.findById(2L);

        assertThat(response.documentCount()).isEqualTo(0);
    }

    @Test
    void findById_throwsNotFound_whenIdMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static CategoryCountView countView(Long categoryId, long count) {
        return new CategoryCountView() {
            @Override public Long getCategoryId() { return categoryId; }
            @Override public Long getCount() { return count; }
        };
    }
}
