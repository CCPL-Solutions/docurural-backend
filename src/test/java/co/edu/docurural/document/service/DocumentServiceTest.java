package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.DeleteDocumentResponse;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentFileContent;
import co.edu.docurural.document.dto.DocumentListResponse;
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
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
class DocumentServiceTest {

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
    DocumentService documentService;

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

        UploadDocumentResponse response = documentService.upload(request, file, AUDIT);

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.category()).isEqualTo("Actas");
        assertThat(response.fileFormat()).isEqualTo("PDF");
        assertThat(response.originalFileName()).isEqualTo("acta.pdf");
        assertThat(response.fileSizeBytes()).isEqualTo(100L);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getFilePath()).isEqualTo("2026/05/uuid.pdf");
        assertThat(captor.getValue().getUploadedBy()).isEqualTo(uploader);

        verify(activityLogService).record(
                eq(ActivityAction.UPLOAD), eq(AUDIT), eq(48L), anyString());
    }

    // ------------------------------------------------------------------
    // upload() - validaciones de entrada
    // ------------------------------------------------------------------

    @Test
    void upload_throwsOnNullAudit() {
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);

        assertThatThrownBy(() -> documentService.upload(request, file, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upload_throwsOnAuditWithNullActorId() {
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);

        assertThatThrownBy(() -> documentService.upload(request, file, new AuditContext(null, "127.0.0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    // upload() - categoría no existe o INACTIVE
    // ------------------------------------------------------------------

    @Test
    void upload_throwsNotFound_whenCategoryDoesNotExist() {
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(99L);
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[10]);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.upload(request, file, AUDIT))
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

        assertThatThrownBy(() -> documentService.upload(request, file, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // upload() - errores de validación de archivo (delega en FileValidationService)
    // ------------------------------------------------------------------

    @Test
    void upload_propagatesPayloadTooLarge_fromValidationService() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        UploadDocumentRequest request = TestFixtures.uploadDocumentRequest(1L);
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[100]);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(fileValidationService.validate(file))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.PAYLOAD_TOO_LARGE, "too large"));

        assertThatThrownBy(() -> documentService.upload(request, file, AUDIT))
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

        assertThatThrownBy(() -> documentService.upload(request, file, AUDIT))
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
                "Acta Consejo Directivo Marzo 2026 - Revisado",
                2L,
                "Secretaría",
                doc.getDocumentDate().plusDays(1),
                "Versión corregida del acta");

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(admin));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateDocumentMetadataResponse response = documentService.updateMetadata(48L, request, AUDIT);

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
                "Acta Consejo Directivo Marzo 2026 - Revisado",
                1L,
                doc.getResponsibleArea(),
                doc.getDocumentDate(),
                null);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(admin));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        documentService.updateMetadata(48L, request, AUDIT);

        assertThat(doc.getDescription()).isEqualTo(originalDescription);
        verify(activityLogService).record(eq(ActivityAction.EDIT_DOC), eq(AUDIT), eq(48L), anyString());
    }

    @Test
    void updateMetadata_returnsSuccessWithoutSavingOrLogging_whenNoChangesDetected() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User admin = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, category, TestFixtures.userEditor(30L));
        UpdateDocumentMetadataRequest request = new UpdateDocumentMetadataRequest(
                doc.getTitle(),
                category.getId(),
                doc.getResponsibleArea(),
                doc.getDocumentDate(),
                null);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(admin));

        UpdateDocumentMetadataResponse response = documentService.updateMetadata(48L, request, AUDIT);

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

        assertThatThrownBy(() -> documentService.updateMetadata(48L, request, editorAudit))
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
        User admin = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, TestFixtures.categoryActive(1L, "Actas"), TestFixtures.userEditor(12L));
        UpdateDocumentMetadataRequest request = TestFixtures.updateDocumentMetadataRequest(9L);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(categoryRepository.findById(9L)).thenReturn(Optional.of(inactiveCategory));

        assertThatThrownBy(() -> documentService.updateMetadata(48L, request, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(documentRepository, never()).save(any());
        verify(activityLogService, never()).record(eq(ActivityAction.EDIT_DOC), any(), anyLong(), anyString());
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

        DocumentDetailResponse response = documentService.findDetailById(48L);

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.title()).isEqualTo(doc.getTitle());
        assertThat(response.category().id()).isEqualTo(1L);
        assertThat(response.category().name()).isEqualTo("Actas");
        assertThat(response.uploadedBy().id()).isEqualTo(ACTOR_ID);
        assertThat(response.uploadedBy().fullName()).isEqualTo(uploader.getFullName());
        assertThat(response.fileFormat()).isEqualTo("PDF");

        verify(activityLogService, never()).record(any(), any(), anyLong(), anyString());
    }

    @Test
    void findDetailById_throwsResourceNotFound_whenDocumentMissing() {
        when(documentRepository.findByIdAndStatus(99L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.findDetailById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // openForView()
    // ------------------------------------------------------------------

    @Test
    void openForView_returnsContentAndLogsActivity_whenDocumentValid() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, category, uploader);
        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3});

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.of(doc));
        when(fileStorageService.load(doc.getFilePath())).thenReturn(resource);

        DocumentFileContent content = documentService.openForView(48L, AUDIT);

        assertThat(content.resource()).isEqualTo(resource);
        assertThat(content.format()).isEqualTo(DocumentFormat.PDF);
        assertThat(content.originalFileName()).isEqualTo(doc.getOriginalFileName());
        assertThat(content.fileSizeBytes()).isEqualTo(doc.getFileSizeBytes());

        verify(activityLogService).record(eq(ActivityAction.VIEW), eq(AUDIT), eq(48L), anyString());
    }

    @Test
    void openForView_throwsResourceNotFound_whenDocumentMissing() {
        when(documentRepository.findByIdAndStatus(99L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.openForView(99L, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(activityLogService, never()).record(any(), any(), anyLong(), anyString());
    }

    @Test
    void openForView_propagatesResourceNotFound_whenFileMissingOnDisk() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, category, uploader);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.of(doc));
        when(fileStorageService.load(doc.getFilePath()))
                .thenThrow(new ResourceNotFoundException("document.file.not-available"));

        assertThatThrownBy(() -> documentService.openForView(48L, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("document.file.not-available");

        verify(activityLogService, never()).record(any(), any(), anyLong(), anyString());
    }

    @Test
    void openForView_throwsIllegalArgument_whenAuditNull() {
        assertThatThrownBy(() -> documentService.openForView(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openForView_throwsIllegalArgument_whenActorUserIdNull() {
        assertThatThrownBy(() -> documentService.openForView(1L, new AuditContext(null, "127.0.0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    // openForDownload()
    // ------------------------------------------------------------------

    @Test
    void openForDownload_returnsContentAndLogsActivity_whenDocumentValid() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, category, uploader);
        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3});

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.of(doc));
        when(fileStorageService.load(doc.getFilePath())).thenReturn(resource);

        DocumentFileContent content = documentService.openForDownload(48L, AUDIT);

        assertThat(content.resource()).isEqualTo(resource);
        assertThat(content.format()).isEqualTo(DocumentFormat.PDF);
        assertThat(content.originalFileName()).isEqualTo(doc.getOriginalFileName());
        assertThat(content.fileSizeBytes()).isEqualTo(doc.getFileSizeBytes());

        verify(activityLogService).record(
                eq(ActivityAction.DOWNLOAD), eq(AUDIT), eq(48L),
                eq("Archivo: " + doc.getOriginalFileName()));
    }

    @Test
    void openForDownload_throwsResourceNotFound_whenDocumentMissing() {
        when(documentRepository.findByIdAndStatus(99L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.openForDownload(99L, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileStorageService, never()).load(anyString());
        verify(activityLogService, never()).record(any(), any(), anyLong(), anyString());
    }

    @Test
    void openForDownload_propagatesResourceNotFound_whenFileMissingOnDisk() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(ACTOR_ID);
        Document doc = TestFixtures.documentActive(48L, category, uploader);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.of(doc));
        when(fileStorageService.load(doc.getFilePath()))
                .thenThrow(new ResourceNotFoundException("document.file.not-available"));

        assertThatThrownBy(() -> documentService.openForDownload(48L, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("document.file.not-available");

        verify(activityLogService, never()).record(any(), any(), anyLong(), anyString());
    }

    @Test
    void openForDownload_throwsIllegalArgument_whenAuditNull() {
        assertThatThrownBy(() -> documentService.openForDownload(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openForDownload_throwsIllegalArgument_whenActorUserIdNull() {
        assertThatThrownBy(() -> documentService.openForDownload(1L, new AuditContext(null, "127.0.0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    // DOC-06/delete
    // ------------------------------------------------------------------

    @Test
    void deleteLogical_marksDocumentAsDeletedAndLogs_whenDocumentIsActive() {
        Category category = TestFixtures.categoryActive(1L, "Actas");
        User uploader = TestFixtures.userAdmin(20L);
        Document doc = TestFixtures.documentActive(48L, category, uploader);

        when(documentRepository.findByIdAndStatus(48L, DocumentStatus.ACTIVE)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DeleteDocumentResponse response = documentService.deleteLogical(48L, AUDIT);

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

        assertThatThrownBy(() -> documentService.deleteLogical(99L, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(documentRepository, never()).save(any());
        verify(activityLogService, never()).record(eq(ActivityAction.DELETE_DOC), any(), anyLong(), anyString());
        verify(fileStorageService, never()).delete(anyString());
    }

    // ------------------------------------------------------------------
    // DOC-01/list
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

        DocumentListResponse response = documentService.list(null, null, null, null);

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

        documentService.list(2, 10, "title", "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(documentRepository).findByStatus(eq(DocumentStatus.ACTIVE), captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(10);
        assertThat(captured.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "title"));
    }

    @Test
    void list_throwsInvalidArgument_whenPageIsZero() {
        assertThatThrownBy(() -> documentService.list(0, null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenPageIsNegative() {
        assertThatThrownBy(() -> documentService.list(-1, null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenSizeIsZero() {
        assertThatThrownBy(() -> documentService.list(null, 0, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenSizeExceedsMax() {
        assertThatThrownBy(() -> documentService.list(null, 51, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenSortByIsNotAllowed() {
        assertThatThrownBy(() -> documentService.list(null, null, "fileSize", null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_throwsInvalidArgument_whenSortDirIsInvalid() {
        assertThatThrownBy(() -> documentService.list(null, null, null, "up"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void list_returnsEmptyDocuments_whenRepositoryReturnsEmptyPage() {
        Page<Document> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L);
        when(documentRepository.findByStatus(eq(DocumentStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(emptyPage);

        DocumentListResponse response = documentService.list(null, null, null, null);

        assertThat(response.totalDocuments()).isEqualTo(0);
        assertThat(response.documents()).isEmpty();
    }

    @Test
    void list_doesNotCallActivityLog() {
        Page<Document> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L);
        when(documentRepository.findByStatus(eq(DocumentStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(emptyPage);

        documentService.list(null, null, null, null);

        verify(activityLogService, never()).record(any(), any(), any(), any());
    }

    // ------------------------------------------------------------------
    // upload() - compensación de archivo huérfano (C-2)
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
            documentService.upload(request, file, AUDIT);

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
            documentService.upload(request, file, AUDIT);

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
            documentService.upload(request, file, AUDIT);

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

            verify(fileStorageService, never()).delete(anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
