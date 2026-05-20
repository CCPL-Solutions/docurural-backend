package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.document.dto.DocumentFileContent;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.storage.FileStorageService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

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
class DocumentContentServiceTest {

    private static final Long ACTOR_ID = 10L;
    private static final AuditContext AUDIT = new AuditContext(ACTOR_ID, "127.0.0.1");

    @Mock
    DocumentRepository documentRepository;
    @Mock
    FileStorageService fileStorageService;
    @Mock
    ActivityLogService activityLogService;
    @Mock
    MessageResolver messageResolver;

    @InjectMocks
    DocumentContentService documentContentService;

    @BeforeEach
    void stubMessageResolver() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageResolver.get(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
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

        DocumentFileContent content = documentContentService.openForView(48L, AUDIT);

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

        assertThatThrownBy(() -> documentContentService.openForView(99L, AUDIT))
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

        assertThatThrownBy(() -> documentContentService.openForView(48L, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("document.file.not-available");

        verify(activityLogService, never()).record(any(), any(), anyLong(), anyString());
    }

    @Test
    void openForView_throwsIllegalArgument_whenAuditNull() {
        assertThatThrownBy(() -> documentContentService.openForView(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openForView_throwsIllegalArgument_whenActorUserIdNull() {
        assertThatThrownBy(() -> documentContentService.openForView(1L, new AuditContext(null, "127.0.0.1")))
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

        DocumentFileContent content = documentContentService.openForDownload(48L, AUDIT);

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

        assertThatThrownBy(() -> documentContentService.openForDownload(99L, AUDIT))
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

        assertThatThrownBy(() -> documentContentService.openForDownload(48L, AUDIT))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("document.file.not-available");

        verify(activityLogService, never()).record(any(), any(), anyLong(), anyString());
    }

    @Test
    void openForDownload_throwsIllegalArgument_whenAuditNull() {
        assertThatThrownBy(() -> documentContentService.openForDownload(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openForDownload_throwsIllegalArgument_whenActorUserIdNull() {
        assertThatThrownBy(() -> documentContentService.openForDownload(1L, new AuditContext(null, "127.0.0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
