package co.edu.docurural.auth.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.auth.dto.LoginRequest;
import co.edu.docurural.auth.dto.LoginResponse;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.shared.dto.MessageResponse;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.security.JwtProperties;
import co.edu.docurural.shared.security.JwtTokenProvider;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final AuditContext LOGIN_AUDIT = new AuditContext(null, "203.0.113.10");
    private static final AuditContext LOGOUT_AUDIT = new AuditContext(42L, "203.0.113.20");

    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    JwtTokenProvider jwtTokenProvider;
    @Mock
    JwtProperties jwtProperties;
    @Mock
    UserRepository userRepository;
    @Mock
    ActivityLogService activityLogService;
    @Mock
    MessageResolver messageResolver;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void stubMessageResolver() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // login()
    // ------------------------------------------------------------------

    @Test
    void login_withValidCredentials_returnsBearerToken_updatesLastLogin_logsLogin() {
        User admin = TestFixtures.userAdmin(10L);
        LoginRequest request = TestFixtures.loginRequest(admin.getEmail(), "plain-password");

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("token-abc");
        when(jwtProperties.getExpirationMs()).thenReturn(1_800_000L);

        LoginResponse response = authService.login(request, LOGIN_AUDIT);

        assertThat(response.token()).isEqualTo("token-abc");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1_800L);
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(10L);
        assertThat(response.user().email()).isEqualTo(admin.getEmail());
        assertThat(response.user().fullName()).isEqualTo(admin.getFullName());
        assertThat(response.user().role()).isEqualTo("ADMIN");

        verify(authenticationManager).authenticate(
                argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken upat
                        && admin.getEmail().equals(upat.getPrincipal())
                        && "plain-password".equals(upat.getCredentials())));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getLastLogin())
                .as("lastLogin should be set before save")
                .isNotNull();

        verify(activityLogService).record(
                eq(ActivityAction.LOGIN),
                eq(new AuditContext(10L, "203.0.113.10")),
                isNull(),
                eq("Inicio de sesión exitoso"));
    }

    @Test
    void login_withBadCredentials_propagatesBadCredentialsException() {
        LoginRequest request = TestFixtures.loginRequest("ana.admin@docurural.edu.co", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request, LOGIN_AUDIT))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userRepository, jwtTokenProvider, jwtProperties, activityLogService);
    }

    @Test
    void login_withDisabledAccount_propagatesDisabledException() {
        LoginRequest request = TestFixtures.loginRequest("ida.inactive@docurural.edu.co", "whatever");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Account disabled"));

        assertThatThrownBy(() -> authService.login(request, LOGIN_AUDIT))
                .isInstanceOf(DisabledException.class);

        verifyNoInteractions(userRepository, jwtTokenProvider, jwtProperties, activityLogService);
    }

    @Test
    void login_authenticatedButUserNotInDb_throwsResourceNotFound() {
        LoginRequest request = TestFixtures.loginRequest("ghost@docurural.edu.co", "any");
        when(userRepository.findByEmail("ghost@docurural.edu.co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, LOGIN_AUDIT))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost@docurural.edu.co");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtTokenProvider, jwtProperties, activityLogService);
    }

    // ------------------------------------------------------------------
    // logout()
    // ------------------------------------------------------------------

    @Test
    void logout_withActorInAudit_incrementsTokenVersion_recordsLogout_returnsMessage() {
        User user = TestFixtures.userAdmin(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.logout(LOGOUT_AUDIT);

        assertThat(response.message()).isEqualTo("auth.logout.success");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTokenVersion()).isEqualTo(1);

        verify(activityLogService).record(
                eq(ActivityAction.LOGOUT),
                eq(LOGOUT_AUDIT),
                isNull(),
                eq("Cierre de sesión"));
    }

    @Test
    void logout_withoutActorInAudit_throwsIllegalArgument() {
        assertThatThrownBy(() -> authService.logout(new AuditContext(null, "203.0.113.20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit.actorUserId");

        verifyNoInteractions(activityLogService);
    }

    @Test
    void logout_withNullAudit_throwsIllegalArgument() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit no puede ser null");

        verifyNoInteractions(activityLogService);
    }
}
