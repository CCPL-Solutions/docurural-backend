package co.edu.docurural.service;

import co.edu.docurural.config.security.CustomUserPrincipal;
import co.edu.docurural.config.security.JwtProperties;
import co.edu.docurural.config.security.JwtTokenProvider;
import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.ActivityAction;
import co.edu.docurural.domain.enums.UserRole;
import co.edu.docurural.domain.enums.UserStatus;
import co.edu.docurural.domain.repository.UserRepository;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.web.dto.auth.LoginRequest;
import co.edu.docurural.web.dto.auth.LoginResponse;
import co.edu.docurural.web.dto.common.MessageResponse;
import co.edu.docurural.web.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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
    MessageSource messageSource;
    @Mock
    HttpServletRequest httpRequest;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void stubMessageSource() {
        lenient().when(messageSource.getMessage(anyString(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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

        LoginResponse response = authService.login(request, httpRequest);

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
                eq(10L),
                isNull(),
                eq("Inicio de sesión exitoso"),
                same(httpRequest));
    }

    @Test
    void login_withBadCredentials_propagatesBadCredentialsException() {
        LoginRequest request = TestFixtures.loginRequest("ana.admin@docurural.edu.co", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userRepository, jwtTokenProvider, jwtProperties, activityLogService);
    }

    @Test
    void login_withDisabledAccount_propagatesDisabledException() {
        LoginRequest request = TestFixtures.loginRequest("ida.inactive@docurural.edu.co", "whatever");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Account disabled"));

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(DisabledException.class);

        verifyNoInteractions(userRepository, jwtTokenProvider, jwtProperties, activityLogService);
    }

    @Test
    void login_authenticatedButUserNotInDb_throwsResourceNotFound() {
        LoginRequest request = TestFixtures.loginRequest("ghost@docurural.edu.co", "any");
        when(userRepository.findByEmail("ghost@docurural.edu.co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost@docurural.edu.co");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtTokenProvider, jwtProperties, activityLogService);
    }

    // ------------------------------------------------------------------
    // logout()
    // ------------------------------------------------------------------

    @Test
    void logout_withAuthenticatedPrincipal_recordsLogout_returnsMessage() {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                42L, "erik.editor@docurural.edu.co",
                UserRole.EDITOR, UserStatus.ACTIVE, null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        MessageResponse response = authService.logout(httpRequest);

        // MessageSource stub returns the key itself (see @BeforeEach).
        assertThat(response.message()).isEqualTo("auth.logout.success");

        verify(activityLogService).record(
                eq(ActivityAction.LOGOUT),
                eq(42L),
                isNull(),
                eq("Cierre de sesión"),
                same(httpRequest));
    }

    @Test
    void logout_withoutAuthentication_throwsIllegalState() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.logout(httpRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay un usuario autenticado");

        verifyNoInteractions(activityLogService);
    }

    @Test
    void logout_withUnexpectedPrincipalType_throwsIllegalState() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "plain-string-principal", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> authService.logout(httpRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no es una instancia de CustomUserPrincipal");

        verifyNoInteractions(activityLogService);
    }
}
