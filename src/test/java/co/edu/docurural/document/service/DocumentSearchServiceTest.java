package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.dto.FilterOptionsResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.shared.util.SortingValidator;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSearchServiceTest {

    private static final Long ACTOR_ID = 10L;
    private static final AuditContext AUDIT = new AuditContext(ACTOR_ID, "127.0.0.1");

    @Mock
    DocumentRepository documentRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ActivityLogService activityLogService;
    @Mock
    MessageResolver messageResolver;

    DocumentSearchServiceImpl documentSearchService;

    @BeforeEach
    void setUp() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageResolver.get(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        documentSearchService = new DocumentSearchServiceImpl(
                documentRepository, categoryRepository, userRepository,
                activityLogService, messageResolver, new SortingValidator(messageResolver));
    }

    private Page<Document> emptyPage() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L);
    }

    private Page<Document> pageOf(Document... docs) {
        return new PageImpl<>(List.of(docs), PageRequest.of(0, 20), docs.length);
    }

    // ------------------------------------------------------------------
    // Validación de q
    // ------------------------------------------------------------------

    @Test
    void search_returns400_whenQHasOneChar() {
        assertThatThrownBy(() -> documentSearchService.search(
                "a", null, null, null, null, null,
                null, null, null, null, false, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void search_returns400_whenQExceeds100Chars() {
        String longQ = "a".repeat(101);
        assertThatThrownBy(() -> documentSearchService.search(
                longQ, null, null, null, null, null,
                null, null, null, null, false, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void search_treatsBlankQAsAbsent_andDoesNotAudit() {
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());

        DocumentListResponse response = documentSearchService.search(
                "   ", null, null, null, null, null,
                null, null, null, null, false, AUDIT);

        assertThat(response.searchTerm()).isNull();
        verify(activityLogService, never()).record(any(), any(), any(), anyString());
    }

    // ------------------------------------------------------------------
    // Validación de rango de fechas
    // ------------------------------------------------------------------

    @Test
    void search_returns400_whenDateFromAfterDateTo() {
        assertThatThrownBy(() -> documentSearchService.search(
                null, null, null,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 30),
                null, null, null, null, null, false, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    // ------------------------------------------------------------------
    // Validaciones de paginación y ordenamiento
    // ------------------------------------------------------------------

    @Test
    void search_throwsInvalidArgument_whenPageIsZero() {
        assertThatThrownBy(() -> documentSearchService.search(
                null, null, null, null, null, null,
                0, null, null, null, false, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void search_throwsInvalidArgument_whenSizeExceedsMax() {
        assertThatThrownBy(() -> documentSearchService.search(
                null, null, null, null, null, null,
                null, 51, null, null, false, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void search_throwsInvalidArgument_whenSortByIsNotAllowed() {
        assertThatThrownBy(() -> documentSearchService.search(
                null, null, null, null, null, null,
                null, null, "fileSize", null, false, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void search_throwsInvalidArgument_whenSortDirIsInvalid() {
        assertThatThrownBy(() -> documentSearchService.search(
                null, null, null, null, null, null,
                null, null, null, "up", false, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    // ------------------------------------------------------------------
    // Auditoría condicional
    // ------------------------------------------------------------------

    @Test
    void search_recordsSearchAudit_whenQProvided() {
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());

        documentSearchService.search(
                "acta", null, null, null, null, null,
                null, null, null, null, false, AUDIT);

        ArgumentCaptor<String> detailCaptor = ArgumentCaptor.forClass(String.class);
        verify(activityLogService).record(
                eq(ActivityAction.SEARCH), eq(AUDIT), isNull(), detailCaptor.capture());
        assertThat(detailCaptor.getValue()).contains("acta").contains("Resultados: 0");
    }

    @Test
    void search_doesNotAudit_whenOnlyFiltersWithoutQ() {
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());
        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(TestFixtures.categoryActive(3L, "Actas")));

        documentSearchService.search(
                null, 3L, null, null, null, null,
                null, null, null, null, false, AUDIT);

        verify(activityLogService, never()).record(any(), any(), any(), anyString());
    }

    @Test
    void search_combinesQAndFilters_andBuildsCompleteDetailString() {
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());
        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(TestFixtures.categoryActive(3L, "Actas")));

        documentSearchService.search(
                "acta", 3L, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31),
                null, null, null, null, null, false, AUDIT);

        ArgumentCaptor<String> detailCaptor = ArgumentCaptor.forClass(String.class);
        verify(activityLogService).record(
                eq(ActivityAction.SEARCH), eq(AUDIT), isNull(), detailCaptor.capture());
        String detail = detailCaptor.getValue();
        assertThat(detail).contains("acta")
                .contains("Actas")
                .contains("2026-01-01")
                .contains("2026-05-31");
    }

    // ------------------------------------------------------------------
    // Manejo de uploadedBy según rol
    // ------------------------------------------------------------------

    @Test
    void search_ignoresUploadedBy_whenActorIsNotAdmin() {
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());

        DocumentListResponse response = documentSearchService.search(
                null, null, null, null, null, 99L,
                null, null, null, null, false, AUDIT);

        // activeFilters es null porque todos los filtros efectivos son null
        assertThat(response.activeFilters()).isNull();
        // userRepository.findById no debe ser llamado porque uploadedByEffective es null
        verify(userRepository, never()).findById(any());
    }

    @Test
    void search_appliesUploadedBy_andResolvesUploadedByName_whenActorIsAdmin() {
        User uploader = TestFixtures.userAdmin(99L);
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());
        when(userRepository.findById(99L)).thenReturn(Optional.of(uploader));

        DocumentListResponse response = documentSearchService.search(
                null, null, null, null, null, 99L,
                null, null, null, null, true, AUDIT);

        assertThat(response.activeFilters()).isNotNull();
        assertThat(response.activeFilters().uploadedByName()).isEqualTo("Ana Admin");
    }

    // ------------------------------------------------------------------
    // Construcción de activeFilters
    // ------------------------------------------------------------------

    @Test
    void search_setsActiveFiltersToNull_whenNoFiltersAndNoQ() {
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());

        DocumentListResponse response = documentSearchService.search(
                null, null, null, null, null, null,
                null, null, null, null, false, AUDIT);

        assertThat(response.searchTerm()).isNull();
        assertThat(response.activeFilters()).isNull();
    }

    @Test
    void search_returnsCategoryNameNull_whenCategoryIdDoesNotExist() {
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        DocumentListResponse response = documentSearchService.search(
                null, 999L, null, null, null, null,
                null, null, null, null, false, AUDIT);

        assertThat(response.activeFilters()).isNotNull();
        assertThat(response.activeFilters().categoryId()).isEqualTo(999L);
        assertThat(response.activeFilters().categoryName()).isNull();
    }

    @Test
    void search_setsSearchTermToTrimmedQ() {
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());

        DocumentListResponse response = documentSearchService.search(
                "  acta  ", null, null, null, null, null,
                null, null, null, null, false, AUDIT);

        assertThat(response.searchTerm()).isEqualTo("acta");
    }

    @Test
    void search_returnsPaginatedResults_withCorrectMetadata() {
        Category cat = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, cat, uploader);
        Page<Document> fakePage = new PageImpl<>(List.of(doc), PageRequest.of(0, 20), 1L);
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(fakePage);

        DocumentListResponse response = documentSearchService.search(
                null, null, null, null, null, null,
                null, null, null, null, false, AUDIT);

        assertThat(response.totalDocuments()).isEqualTo(1);
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.documents()).hasSize(1);
        assertThat(response.documents().get(0).id()).isEqualTo(48L);
    }

    // ------------------------------------------------------------------
    // getFilterOptions()
    // ------------------------------------------------------------------

    @Test
    void getFilterOptions_includesUsers_whenActorIsAdmin() {
        Category actas = TestFixtures.categoryActive(1L, "Actas");
        Category inactiva = TestFixtures.categoryInactive(2L, "Inactiva");
        User admin = TestFixtures.userAdmin(10L);
        when(categoryRepository.findAll()).thenReturn(List.of(actas, inactiva));
        when(userRepository.findAll()).thenReturn(List.of(admin));

        FilterOptionsResponse response = documentSearchService.getFilterOptions(true);

        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).name()).isEqualTo("Actas");
        assertThat(response.users()).isNotNull();
        assertThat(response.users()).hasSize(1);
        assertThat(response.users().get(0).fullName()).isEqualTo("Ana Admin");
    }

    @Test
    void getFilterOptions_omitsUsers_whenActorIsNotAdmin() {
        Category actas = TestFixtures.categoryActive(1L, "Actas");
        when(categoryRepository.findAll()).thenReturn(List.of(actas));

        FilterOptionsResponse response = documentSearchService.getFilterOptions(false);

        assertThat(response.categories()).hasSize(1);
        assertThat(response.users()).isNull();
        verify(userRepository, never()).findAll();
    }

    @Test
    void getFilterOptions_filtersOutInactiveCategoriesAndInactiveUsers() {
        Category activa = TestFixtures.categoryActive(1L, "Actas");
        Category inactiva = TestFixtures.categoryInactive(2L, "Inactiva");
        User activeUser = TestFixtures.userAdmin(10L);
        User inactiveUser = TestFixtures.userInactive(20L);
        when(categoryRepository.findAll()).thenReturn(List.of(activa, inactiva));
        when(userRepository.findAll()).thenReturn(List.of(activeUser, inactiveUser));

        FilterOptionsResponse response = documentSearchService.getFilterOptions(true);

        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).id()).isEqualTo(1L);
        assertThat(response.users()).hasSize(1);
        assertThat(response.users().get(0).id()).isEqualTo(10L);
    }
}
