package co.edu.docurural.service;

import co.edu.docurural.domain.entity.ActivityLog;
import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.enums.ActivityAction;
import co.edu.docurural.domain.repository.ActivityLogRepository;
import co.edu.docurural.domain.repository.DocumentRepository;
import co.edu.docurural.domain.repository.UserRepository;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.web.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
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
import static org.mockito.Mockito.never;
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
    @Mock
    HttpServletRequest httpRequest;

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
                null, 1L, null, "detail", httpRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action");

        verifyNoInteractions(activityLogRepository, userRepository, documentRepository);
    }

    @Test
    void record_withNullUserId_throwsIllegalArgument() {
        assertThatThrownBy(() -> activityLogService.record(
                ActivityAction.LOGIN, null, null, "detail", httpRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");

        verifyNoInteractions(activityLogRepository, userRepository, documentRepository);
    }

    @Test
    void record_withMissingUser_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityLogService.record(
                ActivityAction.LOGIN, 99L, null, "detail", httpRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(activityLogRepository, documentRepository);
    }

    @Test
    void record_withDocumentId_andMissingDocument_throwsResourceNotFound() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityLogService.record(
                ActivityAction.DOWNLOAD, 5L, 77L, "detail", httpRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(activityLogRepository);
    }

    // ------------------------------------------------------------------
    // IP resolution
    // ------------------------------------------------------------------

    @Test
    void record_withNullRequest_persistsEntryWithNullIp() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        activityLogService.record(ActivityAction.LOGIN, 5L, null, "Inicio sesión", null);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isNull();
    }

    @Test
    void record_withXForwardedFor_singleIp_usesHeader() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42");
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        activityLogService.record(ActivityAction.LOGIN, 5L, null, "detail", httpRequest);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.42");

        verify(httpRequest, never()).getRemoteAddr();
    }

    @Test
    void record_withXForwardedFor_commaSeparated_usesFirst() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For"))
                .thenReturn("203.0.113.42, 10.0.0.1, 10.0.0.2");
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        activityLogService.record(ActivityAction.LOGIN, 5L, null, "detail", httpRequest);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.42");
    }

    @Test
    void record_withXForwardedFor_blank_fallsBackToRemoteAddr() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.50");
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        activityLogService.record(ActivityAction.LOGIN, 5L, null, "detail", httpRequest);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.1.50");
    }

    @Test
    void record_withIpLongerThan45chars_truncates() {
        User user = TestFixtures.userEditor(5L);
        String longIp = "a".repeat(60);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(longIp);
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        activityLogService.record(ActivityAction.LOGIN, 5L, null, "detail", httpRequest);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress())
                .hasSize(45)
                .isEqualTo(longIp.substring(0, 45));
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    void record_withoutDocumentAndValidUser_persistsAndReturnsSavedEntity() {
        User user = TestFixtures.userEditor(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ActivityLog result = activityLogService.record(
                ActivityAction.CREATE_USER, 5L, null, "Usuario creado: 99", httpRequest);

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
