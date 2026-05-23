package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.DeleteDocumentResponse;
import co.edu.docurural.document.dto.UpdateDocumentMetadataRequest;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponse;
import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.storage.FileStorageService;
import co.edu.docurural.document.storage.StoredFile;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentCommandServiceTest {

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
    FileValidationService fileValidationService;
    @Mock
    FileStorageService fileStorageService;
    @Mock
    MessageResolver messageResolver;

    @InjectMocks
    DocumentCommandServiceImpl documentCommandService;

    @BeforeEach
    void stubMessageResolver() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageResolver.get(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // upload() - happy path
    // ------------------------------------------------------------------

    @Test
    void upload_persistsDocumentAndLogsActivity_whenAllValid() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(fileValidationService.validate(file)).thenReturn(DocumentFormat.PDF);
        when(fileStorageService.store(file, DocumentFormat.PDF))
                .thenReturn(new StoredFile("2026/05/uuid.pdf"));
        when(userRepository.getReferenceById(ACTOR_ID)).thenReturn(uploader);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(48L);
            return d;
        });

        UploadDocumentResponse response = documentCommandService.upload(request, file, AUDIT);

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.category()).isEqualTo("Actas");
        assertThat(response.fileFormat()).isEqualTo("PDF");
        assertThat(response.originalFileName()).isEqualTo("acta.pdf");
        assertThat(response.fileSizeBytes()).isEqualTo(100L);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getFilePath()).isEqualTo("2026/05/uuid.pdf");
        assertThat(captor.getValue().getUploadedBy()).isEqualTo(uploader);

        verify(activityLogService).record(eq(ActivityAction.UPLOAD), eq(AUDIT), eq(48L), anyString());
    }

    @Test
    void upload_throwsOnNullAudit() {
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);

        assertThatThrownBy(() -> documentCommandService.upload(request, file, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upload_throwsOnAuditWithNullActorId() {
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);

        assertThatThrownBy(() -> documentCommandService.upload(request, file, new AuditContext(null, "127.0.0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upload_throwsNotFound_whenCategoryDoesNotExist() {
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(99L);
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentCommandService.upload(request, file, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileValidationService, never()).validate(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void upload_throwsNotFound_whenCategoryIsInactive() {
        Category inactive = TestFixtures.categoryInactive(1L, "Actas");
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> documentCommandService.upload(request, file, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void upload_propagatesPayloadTooLarge_fromValidationService() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[100]);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(fileValidationService.validate(file))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.PAYLOAD_TOO_LARGE, "too large"));

        assertThatThrownBy(() -> documentCommandService.upload(request, file, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.PAYLOAD_TOO_LARGE);

        verify(documentRepository, never()).save(any());
    }

    @Test
    void upload_propagatesUnsupportedMediaType_fromValidationService() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "hack.pdf", "text/plain", new byte[100]);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(fileValidationService.validate(file))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.UNSUPPORTED_MEDIA_TYPE, "bad mime"));

        assertThatThrownBy(() -> documentCommandService.upload(request, file, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    // ------------------------------------------------------------------
    // updateMetadata()
    // ------------------------------------------------------------------

    @Test
    void updateMetadata_updatesAndLogs_whenAdminAndChangesDetected() {
        Category currentCategory = TestFixtures.categoryActive(1L, "Actas");
        Category newCategory = TestFixtures.categoryActive(2L, "Informes");
        User admin = TestFixtures.userAdmin(ACTOR_ID);
        User uploader = TestFixtures.userEditor(33L);
        Document doc = TestFixtures.documentActive(48L, currentCategory, uploader);
        UpdateDocumentMetadataRequest request = new UpdateDocumentMetadataRequest(
                "Acta Consejo Directivo Marzo 2026 - Revisado", 2L, "Secretaría",
                doc.getDocumentDate().plusDays(1), "Versión corregida del acta");

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(admin));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateDocumentMetadataResponse response = documentCommandService.updateMetadata(48L, request, AUDIT);

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.title()).isEqualTo("Acta Consejo Directivo Marzo 2026 - Revisado");
        assertThat(response.category()).isEqualTo("Informes");
        assertThat(response.responsibleArea()).isEqualTo("Secretaría");

        verify(documentRepository).save(doc);
        verify(activityLogService).record(eq(ActivityAction.EDIT_DOC), eq(AUDIT), eq(48L), anyString());
    }

    @Test
    void updateMetadata_keepsDescription_whenDescriptionOmitted() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User admin = TestFixtures.userAdmin(ACTOR_ID);
        User uploader = TestFixtures.userEditor(20L);
        Document doc = TestFixtures.documentActive(48L, category, uploader);
        String originalDescription = doc.getDescription();
        UpdateDocumentMetadataRequest request = new UpdateDocumentMetadataRequest(
                "Acta Consejo Directivo Marzo 2026 - Revisado", 1L,
                doc.getResponsibleArea(), doc.getDocumentDate(), null);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(admin));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        documentCommandService.updateMetadata(48L, request, AUDIT);

        assertThat(doc.getDescription()).isEqualTo(originalDescription);
        verify(activityLogService).record(eq(ActivityAction.EDIT_DOC), eq(AUDIT), eq(48L), anyString());
    }

    @Test
    void updateMetadata_returnsSuccessWithoutSavingOrLogging_whenNoChangesDetected() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User admin = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, category, TestFixtures.userEditor(30L));
        UpdateDocumentMetadataRequest request = new UpdateDocumentMetadataRequest(
                doc.getTitle(), category.getId(), doc.getResponsibleArea(), doc.getDocumentDate(), null);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(admin));

        UpdateDocumentMetadataResponse response = documentCommandService.updateMetadata(48L, request, AUDIT);

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.message()).isEqualTo("document.updated.no-changes");
        verify(documentRepository, never()).save(any());
        verify(activityLogService, never()).record(eq(ActivityAction.EDIT_DOC), any(), anyLong(), anyString());
    }

    @Test
    void updateMetadata_throwsForbidden_whenEditorAttemptsToEditForeignDocument() {
        Long editorActorId = 50L;
        AuditContext editorAudit = new AuditContext(editorActorId, "127.0.0.1");
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User editor = TestFixtures.userEditor(editorActorId);
        User foreignUploader = TestFixtures.userEditor(77L);
        Document doc = TestFixtures.documentActive(48L, category, foreignUploader);
        UpdateDocumentMetadataRequest request = TestFixtures.updateDocumentMetadataRequest(1L);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(editorActorId)).thenReturn(Optional.of(editor));
        when(messageResolver.get("document.edit.forbidden")).thenReturn("No tiene permisos para editar este documento");

        assertThatThrownBy(() -> documentCommandService.updateMetadata(48L, request, editorAudit))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException businessEx = (BusinessRuleException) ex;
                    assertThat(businessEx.getCode()).isEqualTo(BusinessErrorCode.FORBIDDEN);
                    assertThat(businessEx.getMessage()).isEqualTo("No tiene permisos para editar este documento");
                });

        verify(documentRepository, never()).save(any());
        verify(activityLogService, never()).record(eq(ActivityAction.EDIT_DOC), any(), anyLong(), anyString());
    }

    @Test
    void updateMetadata_throwsNotFound_whenCategoryIsInactive() {
        Category inactiveCategory = TestFixtures.categoryInactive(9L, "Actas");
        Document doc = TestFixtures.documentActive(48L, TestFixtures.categoryActive(1L, "Actas"), TestFixtures.userEditor(12L));
        UpdateDocumentMetadataRequest request = TestFixtures.updateDocumentMetadataRequest(9L);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(9L)).thenReturn(Optional.of(inactiveCategory));

        assertThatThrownBy(() -> documentCommandService.updateMetadata(48L, request, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(documentRepository, never()).save(any());
        verify(activityLogService, never()).record(eq(ActivityAction.EDIT_DOC), any(), anyLong(), anyString());
    }

    // ------------------------------------------------------------------
    // deleteLogical()
    // ------------------------------------------------------------------

    @Test
    void deleteLogical_marksDocumentAsDeletedAndLogs_whenDocumentIsActive() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(20L);
        Document doc = TestFixtures.documentActive(48L, category, uploader);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DeleteDocumentResponse response = documentCommandService.deleteLogical(48L, AUDIT);

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.message()).isEqualTo("document.deleted.success");
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DELETED);

        verify(documentRepository).save(doc);
        verify(activityLogService).record(
                eq(ActivityAction.DELETE_DOC), eq(AUDIT), eq(48L), eq("Título: " + doc.getTitle()));
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void deleteLogical_throwsNotFound_whenDocumentMissingOrAlreadyDeleted() {
        when(documentRepository.findByIdAndStatus(99L, DocumentStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentCommandService.deleteLogical(99L, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(documentRepository, never()).save(any());
        verify(activityLogService, never()).record(eq(ActivityAction.DELETE_DOC), any(), anyLong(), anyString());
        verify(fileStorageService, never()).delete(anyString());
    }

    // ------------------------------------------------------------------
    // Compensación de archivo huérfano (C-2)
    // ------------------------------------------------------------------

    @Test
    void upload_deletesStoredFile_whenTransactionIsRolledBack() throws Exception {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(fileValidationService.validate(file)).thenReturn(DocumentFormat.PDF);
        when(fileStorageService.store(file, DocumentFormat.PDF)).thenReturn(new StoredFile("2026/05/uuid.pdf"));
        when(userRepository.getReferenceById(ACTOR_ID)).thenReturn(uploader);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(48L);
            return d;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            documentCommandService.upload(request, file, AUDIT);

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).isNotEmpty();
            synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(fileStorageService).delete("2026/05/uuid.pdf");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void upload_deletesStoredFile_whenTransactionCompletesWithUnknownStatus() throws Exception {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(fileValidationService.validate(file)).thenReturn(DocumentFormat.PDF);
        when(fileStorageService.store(file, DocumentFormat.PDF)).thenReturn(new StoredFile("2026/05/uuid.pdf"));
        when(userRepository.getReferenceById(ACTOR_ID)).thenReturn(uploader);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(48L);
            return d;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            documentCommandService.upload(request, file, AUDIT);

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).isNotEmpty();
            synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN));

            verify(fileStorageService).delete("2026/05/uuid.pdf");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void upload_keepsStoredFile_whenTransactionCommitsSuccessfully() throws Exception {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(fileValidationService.validate(file)).thenReturn(DocumentFormat.PDF);
        when(fileStorageService.store(file, DocumentFormat.PDF)).thenReturn(new StoredFile("2026/05/uuid.pdf"));
        when(userRepository.getReferenceById(ACTOR_ID)).thenReturn(uploader);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(48L);
            return d;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            documentCommandService.upload(request, file, AUDIT);

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

            verify(fileStorageService, never()).delete(anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
