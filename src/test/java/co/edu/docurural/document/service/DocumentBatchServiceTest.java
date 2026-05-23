package co.edu.docurural.document.service;

import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.BatchUploadDocumentRequest;
import co.edu.docurural.document.dto.BatchUploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentBatchServiceTest {

    private static final Long ACTOR_ID = 10L;
    private static final AuditContext AUDIT = new AuditContext(ACTOR_ID, "127.0.0.1");
    private static final LocalDate DOC_DATE = LocalDate.of(2026, 3, 15);

    @Mock DocumentCommandService documentService;
    @Mock CategoryRepository categoryRepository;
    @Mock MessageResolver messageResolver;

    @InjectMocks DocumentBatchServiceImpl batchService;

    @BeforeEach
    void stubCommon() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageResolver.get(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    void uploadBatch_returnsAllSuccessful_whenAllFilesValid() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Document doc1 = TestFixtures.documentActive(48L, category, TestFixtures.userAdmin(ACTOR_ID));
        Document doc2 = TestFixtures.documentActive(49L, category, TestFixtures.userAdmin(ACTOR_ID));

        MultipartFile[] files = {
                pdf("acta_enero.pdf"),
                pdf("acta_febrero.pdf")
        };

        when(documentService.uploadSingleForBatch(eq(files[0]), any(), eq(1L), any(), any(), eq(AUDIT))).thenReturn(doc1);
        when(documentService.uploadSingleForBatch(eq(files[1]), any(), eq(1L), any(), any(), eq(AUDIT))).thenReturn(doc2);

        BatchUploadDocumentRequest request = request(1L, List.of("Acta Enero", "Acta Febrero"));
        BatchUploadDocumentResponse response = batchService.uploadBatch(request, files, AUDIT);

        assertThat(response.totalReceived()).isEqualTo(2);
        assertThat(response.totalSuccessful()).isEqualTo(2);
        assertThat(response.totalFailed()).isEqualTo(0);
        assertThat(response.results()).allMatch(r -> r.success() && r.errorMessage() == null);
        assertThat(response.results().get(0).documentId()).isEqualTo(48L);
        assertThat(response.results().get(1).documentId()).isEqualTo(49L);
    }

    @Test
    void uploadBatch_usesOriginalFilenameAsTitle_whenTitleListIsNull() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Document doc = TestFixtures.documentActive(48L, category, TestFixtures.userAdmin(ACTOR_ID));
        when(documentService.uploadSingleForBatch(any(), eq("acta.pdf"), any(), any(), any(), any())).thenReturn(doc);

        MultipartFile[] files = {pdf("acta.pdf")};
        BatchUploadDocumentResponse response = batchService.uploadBatch(request(1L, null), files, AUDIT);

        assertThat(response.totalSuccessful()).isEqualTo(1);
        verify(documentService).uploadSingleForBatch(any(), eq("acta.pdf"), any(), any(), any(), any());
    }

    @Test
    void uploadBatch_usesOriginalFilenameForPositionsMissingInTitleList() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Document doc1 = TestFixtures.documentActive(48L, category, TestFixtures.userAdmin(ACTOR_ID));
        Document doc2 = TestFixtures.documentActive(49L, category, TestFixtures.userAdmin(ACTOR_ID));
        MultipartFile[] files = {pdf("enero.pdf"), pdf("febrero.pdf")};

        when(documentService.uploadSingleForBatch(eq(files[0]), eq("Título provisto"), any(), any(), any(), any())).thenReturn(doc1);
        when(documentService.uploadSingleForBatch(eq(files[1]), eq("febrero.pdf"), any(), any(), any(), any())).thenReturn(doc2);

        BatchUploadDocumentResponse response = batchService.uploadBatch(
                request(1L, List.of("Título provisto")), files, AUDIT);

        assertThat(response.totalSuccessful()).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // Fallos individuales — best-effort
    // ------------------------------------------------------------------

    @Test
    void uploadBatch_reportsPartialFailure_whenOneFileFails() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Document doc = TestFixtures.documentActive(48L, category, TestFixtures.userAdmin(ACTOR_ID));
        MultipartFile[] files = {pdf("bueno.pdf"), pdf("malo.pdf")};

        when(documentService.uploadSingleForBatch(eq(files[0]), any(), any(), any(), any(), any())).thenReturn(doc);
        when(documentService.uploadSingleForBatch(eq(files[1]), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.PAYLOAD_TOO_LARGE, "too large"));

        BatchUploadDocumentResponse response = batchService.uploadBatch(request(1L, null), files, AUDIT);

        assertThat(response.totalReceived()).isEqualTo(2);
        assertThat(response.totalSuccessful()).isEqualTo(1);
        assertThat(response.totalFailed()).isEqualTo(1);
        assertThat(response.results().get(0).success()).isTrue();
        assertThat(response.results().get(1).success()).isFalse();
        assertThat(response.results().get(1).errorMessage()).isEqualTo("too large");
    }

    @Test
    void uploadBatch_reportsFailure_whenStorageExceptionThrown() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        MultipartFile[] files = {pdf("doc.pdf")};
        when(documentService.uploadSingleForBatch(any(), any(), any(), any(), any(), any()))
                .thenThrow(new FileStorageException("disk full"));

        BatchUploadDocumentResponse response = batchService.uploadBatch(request(1L, null), files, AUDIT);

        assertThat(response.totalFailed()).isEqualTo(1);
        assertThat(response.results().get(0).errorMessage()).isEqualTo("disk full");
    }

    @Test
    void uploadBatch_continuesProcessingRemainingFiles_afterOneFailure() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Document doc = TestFixtures.documentActive(50L, category, TestFixtures.userAdmin(ACTOR_ID));
        MultipartFile[] files = {pdf("a.pdf"), pdf("b.pdf"), pdf("c.pdf")};

        when(documentService.uploadSingleForBatch(eq(files[0]), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.UNSUPPORTED_MEDIA_TYPE, "bad mime"));
        when(documentService.uploadSingleForBatch(eq(files[1]), any(), any(), any(), any(), any())).thenReturn(doc);
        when(documentService.uploadSingleForBatch(eq(files[2]), any(), any(), any(), any(), any())).thenReturn(doc);

        BatchUploadDocumentResponse response = batchService.uploadBatch(request(1L, null), files, AUDIT);

        assertThat(response.totalSuccessful()).isEqualTo(2);
        assertThat(response.totalFailed()).isEqualTo(1);
        verify(documentService, times(3)).uploadSingleForBatch(any(), any(), any(), any(), any(), any());
    }

    // ------------------------------------------------------------------
    // Validaciones del lote
    // ------------------------------------------------------------------

    @Test
    void uploadBatch_throwsInvalidArgument_whenFilesIsNull() {
        assertThatThrownBy(() -> batchService.uploadBatch(request(1L, null), null, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT);
    }

    @Test
    void uploadBatch_throwsInvalidArgument_whenFilesIsEmpty() {
        assertThatThrownBy(() -> batchService.uploadBatch(request(1L, null), new MultipartFile[0], AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT);
    }

    @Test
    void uploadBatch_throwsInvalidArgument_whenExceedsMaxFiles() {
        MultipartFile[] files = new MultipartFile[DocumentBatchService.MAX_FILES_PER_BATCH + 1];
        for (int i = 0; i < files.length; i++) {
            files[i] = pdf("file" + i + ".pdf");
        }

        assertThatThrownBy(() -> batchService.uploadBatch(request(1L, null), files, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT);

        verify(documentService, never()).uploadSingleForBatch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void uploadBatch_throwsInvalidArgument_whenTitlesExceedFilesCount() {
        MultipartFile[] files = {pdf("a.pdf")};
        BatchUploadDocumentRequest request = request(1L, List.of("Título 1", "Título 2 de más"));

        assertThatThrownBy(() -> batchService.uploadBatch(request, files, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT);
    }

    // ------------------------------------------------------------------
    // Categoría inválida — aborta todo el lote
    // ------------------------------------------------------------------

    @Test
    void uploadBatch_throwsNotFound_whenCategoryDoesNotExist() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        MultipartFile[] files = {pdf("doc.pdf")};

        assertThatThrownBy(() -> batchService.uploadBatch(request(99L, null), files, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(documentService, never()).uploadSingleForBatch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void uploadBatch_throwsNotFound_whenCategoryIsInactive() {
        Category inactive = TestFixtures.categoryInactive(1L, "Actas");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(inactive));
        MultipartFile[] files = {pdf("doc.pdf")};

        assertThatThrownBy(() -> batchService.uploadBatch(request(1L, null), files, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // Contexto de auditoría
    // ------------------------------------------------------------------

    @Test
    void uploadBatch_throwsIllegalArgument_whenAuditIsNull() {
        assertThatThrownBy(() -> batchService.uploadBatch(request(1L, null), new MultipartFile[]{pdf("f.pdf")}, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit no puede ser null");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static MultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf", new byte[100]);
    }

    private static BatchUploadDocumentRequest request(Long categoryId, List<String> titles) {
        return new BatchUploadDocumentRequest(categoryId, "Rectoría", titles);
    }
}
