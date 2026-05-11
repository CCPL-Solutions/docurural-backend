package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentViewContent;
import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.storage.FileStorageService;
import co.edu.docurural.document.storage.StoredFile;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.domain.entity.User;
import co.edu.docurural.shared.domain.repository.UserRepository;
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
import org.springframework.mock.web.MockMultipartFile;

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

        DocumentViewContent content = documentService.openForView(48L, AUDIT);

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
}
