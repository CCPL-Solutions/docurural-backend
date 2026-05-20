package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentQueryServiceTest {

    private static final Long ACTOR_ID = 10L;
    private static final AuditContext AUDIT = new AuditContext(ACTOR_ID, "127.0.0.1");

    @Mock
    DocumentRepository documentRepository;
    @Mock
    MessageResolver messageResolver;
    @Mock
    ActivityLogService activityLogService;

    @InjectMocks
    DocumentQueryService documentQueryService;

    @BeforeEach
    void stubMessageResolver() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageResolver.get(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // findDetailById()
    // ------------------------------------------------------------------

    @Test
    void findDetailById_returnsDetail_whenDocumentActive() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, category, uploader);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.of(doc));

        DocumentDetailResponse response = documentQueryService.findDetailById(48L);

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.title()).isEqualTo(doc.getTitle());
        assertThat(response.category().id()).isEqualTo(1L);
        assertThat(response.category().name()).isEqualTo("Actas");
        assertThat(response.uploadedBy().id()).isEqualTo(ACTOR_ID);
        assertThat(response.uploadedBy().fullName()).isEqualTo(uploader.getFullName());
        assertThat(response.fileFormat()).isEqualTo("PDF");

        verify(activityLogService, never()).record(any(), any(), any(), anyString());
    }

    @Test
    void findDetailById_throwsResourceNotFound_whenDocumentMissing() {
        when(documentRepository.findByIdAndStatus(99L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentQueryService.findDetailById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // list()
    // ------------------------------------------------------------------

    @Test
    void list_returnsPagedDocuments_whenParamsAreDefaults() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(20L);
        Document doc1 = TestFixtures.documentActive(47L, category, uploader);
        Document doc2 = TestFixtures.documentActive(48L, category, uploader);

        PageRequest expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> fakePage = new PageImpl<>(List.of(doc1, doc2), expectedPageable, 2L);
        when(documentRepository.findByStatus(eq(DocumentStatus.ACTIVE), any(Pageable.class))).thenReturn(fakePage);

        DocumentListResponse response = documentQueryService.list(null, null, null, null);

        assertThat(response.totalDocuments()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.documents()).hasSize(2);
        assertThat(response.documents().get(0).id()).isEqualTo(47L);
    }

    @Test
    void list_appliesCustomSortAndPage_whenParamsProvided() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(20L);
        Document doc = TestFixtures.documentActive(1L, category, uploader);

        Page<Document> fakePage = new PageImpl<>(List.of(doc),
                PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "title")), 11L);
        when(documentRepository.findByStatus(eq(DocumentStatus.ACTIVE), any(Pageable.class))).thenReturn(fakePage);

        documentQueryService.list(2, 10, "title", "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(documentRepository).findByStatus(eq(DocumentStatus.ACTIVE), captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(10);
        assertThat(captured.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "title"));
    }

    @Test
    void list_throwsInvalidArgument_whenPageIsZero() {
        assertThatThrownBy(() -> documentQueryService.list(0, null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenPageIsNegative() {
        assertThatThrownBy(() -> documentQueryService.list(-1, null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenSizeIsZero() {
        assertThatThrownBy(() -> documentQueryService.list(null, 0, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenSizeExceedsMax() {
        assertThatThrownBy(() -> documentQueryService.list(null, 51, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenSortByIsNotAllowed() {
        assertThatThrownBy(() -> documentQueryService.list(null, null, "fileSize", null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenSortDirIsInvalid() {
        assertThatThrownBy(() -> documentQueryService.list(null, null, null, "up"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_returnsEmptyDocuments_whenRepositoryReturnsEmptyPage() {
        Page<Document> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L);
        when(documentRepository.findByStatus(eq(DocumentStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(emptyPage);

        DocumentListResponse response = documentQueryService.list(null, null, null, null);

        assertThat(response.totalDocuments()).isEqualTo(0);
        assertThat(response.documents()).isEmpty();
    }

    @Test
    void list_doesNotCallActivityLog() {
        Page<Document> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L);
        when(documentRepository.findByStatus(eq(DocumentStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(emptyPage);

        documentQueryService.list(null, null, null, null);

        verify(activityLogService, never()).record(any(), any(), any(), any());
    }
}
