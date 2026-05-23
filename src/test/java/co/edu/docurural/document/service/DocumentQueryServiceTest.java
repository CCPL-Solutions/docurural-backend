package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    DocumentQueryServiceImpl documentQueryService;

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
}
