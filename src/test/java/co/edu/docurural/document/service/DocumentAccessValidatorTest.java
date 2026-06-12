package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.enums.SensitivityLevel;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.security.CustomUserPrincipal;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentAccessValidatorTest {

    private static final AuditContext AUDIT = new AuditContext(10L, "127.0.0.1");

    @Mock
    ActivityLogService activityLogService;
    @Mock
    MessageResolver messageResolver;
    @Mock
    Authentication authentication;
    @Mock
    SecurityContext securityContext;
    @Mock
    CustomUserPrincipal principal;

    @InjectMocks
    DocumentAccessValidator documentAccessValidator;

    @BeforeEach
    void setUpSecurityContext() {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validateAccess_doesNothing_whenUserIsAdmin() {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getRole()).thenReturn(UserRole.ADMIN);

        Document doc = Document.builder()
                .id(1L)
                .sensitivityLevel(SensitivityLevel.CONFIDENTIAL)
                .build();

        documentAccessValidator.validateAccess(doc, AUDIT);

        verify(activityLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void validateAccess_doesNothing_whenDocumentIsInternal() {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getRole()).thenReturn(UserRole.READER);

        Document doc = Document.builder()
                .id(1L)
                .sensitivityLevel(SensitivityLevel.INTERNAL)
                .build();

        documentAccessValidator.validateAccess(doc, AUDIT);

        verify(activityLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void validateAccess_throwsExceptionAndLogs_whenDocumentRestrictedAndUserReader() {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getRole()).thenReturn(UserRole.READER);

        Document doc = Document.builder()
                .id(1L)
                .sensitivityLevel(SensitivityLevel.RESTRICTED)
                .build();

        assertThatThrownBy(() -> documentAccessValidator.validateAccess(doc, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("document.access.forbidden");

        String expectedDetail = "document_id: 1, sensitivity_level: RESTRICTED, user_role: READER";
        verify(activityLogService).record(ActivityAction.ACCESS_DENIED, AUDIT, 1L, expectedDetail);
    }

    @Test
    void validateAccess_throwsExceptionAndLogs_whenDocumentConfidentialAndUserEditor() {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getRole()).thenReturn(UserRole.EDITOR);

        Document doc = Document.builder()
                .id(2L)
                .sensitivityLevel(SensitivityLevel.CONFIDENTIAL)
                .build();

        assertThatThrownBy(() -> documentAccessValidator.validateAccess(doc, AUDIT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("document.access.forbidden");

        String expectedDetail = "document_id: 2, sensitivity_level: CONFIDENTIAL, user_role: EDITOR";
        verify(activityLogService).record(ActivityAction.ACCESS_DENIED, AUDIT, 2L, expectedDetail);
    }
}

