package co.edu.docurural.category.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.dto.CreateCategoryRequest;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
