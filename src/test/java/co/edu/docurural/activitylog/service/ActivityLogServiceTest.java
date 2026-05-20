package co.edu.docurural.activitylog.service;

import co.edu.docurural.activitylog.entity.ActivityLog;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.repository.ActivityLogRepository;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    ActivityLogRepository activityLogRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    DocumentRepository documentRepository;
    @Mock
    MessageSource messageSource;

    @InjectMocks
    ActivityLogService activityLogService;

    @BeforeEach
    void stubMessageSource() {
        lenient().when(messageSource.getMessage(anyString(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // Guard clauses
    // ------------------------------------------------------------------

    @Test
    void record_withNullAction_throwsIllegalArgument() {
        assertThatThrownBy(() -> activityLogService.record(
                null, new AuditContext(1L, "203.0.113.10"), null, "detail"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action");

        verifyNoInteractions(activityLogRepository, userRepository, documentRepository);
    }

    @Test
    void record_withNullUserId_throwsIllegalArgument() {
        assertThatThrownBy(() -> activityLogService.record(
                ActivityAction.LOGIN, new AuditContext(null, "203.0.113.10"), null, "detail"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit.actorUserId");

        verifyNoInteractions(activityLogRepository, userRepository, documentRepository);
    }

    @Test
    void record_withNullAudit_throwsIllegalArgument() {
        assertThatThrownBy(() -> activityLogService.record(
                ActivityAction.LOGIN, null, null, "detail"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit");

        verifyNoInteractions(activityLogRepository, userRepository, documentRepository);
    }

    @Test
    void record_withMissingUser_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityLogService.record(
                ActivityAction.LOGIN, new AuditContext(99L, "203.0.113.10"), null, "detail"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(activityLogRepository, documentRepository);
    }

    @Test
    void record_withDocumentId_andMissingDocument_throwsResourceNotFound() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityLogService.record(
                ActivityAction.DOWNLOAD, new AuditContext(5L, "203.0.113.10"), 77L, "detail"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(activityLogRepository);
    }

    @Test
    void record_withNullClientIp_persistsEntryWithNullIp() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        activityLogService.record(ActivityAction.LOGIN, new AuditContext(5L, null), null, "Inicio sesión");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isNull();
    }

    @Test
    void record_withClientIp_persistsIpAsProvidedByAuditContext() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        activityLogService.record(
                ActivityAction.LOGIN,
                new AuditContext(5L, "203.0.113.42"),
                null,
                "detail");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.42");
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    void record_withoutDocumentAndValidUser_persistsAndReturnsSavedEntity() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ActivityLog result = activityLogService.record(
                ActivityAction.CREATE_USER,
                new AuditContext(5L, "127.0.0.1"),
                null,
                "Usuario creado: 99");

        assertThat(result).isNotNull();

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        ActivityLog saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getAction()).isEqualTo(ActivityAction.CREATE_USER);
        assertThat(saved.getDocument()).isNull();
        assertThat(saved.getDetail()).isEqualTo("Usuario creado: 99");
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");

        verifyNoInteractions(documentRepository);
        // el servicio retorna lo que devuelve save
        assertThat(result).isSameAs(saved);
    }
}
