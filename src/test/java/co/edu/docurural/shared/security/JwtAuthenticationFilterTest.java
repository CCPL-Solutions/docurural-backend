package co.edu.docurural.shared.security;

import co.edu.docurural.shared.domain.entity.User;
import co.edu.docurural.shared.domain.enums.UserRole;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.support.TestFixtures;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    UserRepository userRepository;

    @Mock
    FilterChain filterChain;

    @InjectMocks
    JwtAuthenticationFilter filter;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_withNoAuthorizationHeader_chainContinuesWithoutTouchingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withNonBearerHeader_chainContinuesWithoutTouchingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withBearerPrefixAndBlankToken_chainContinuesWithoutTouchingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withValidToken_setsAuthenticationWithCorrectPrincipal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User admin = TestFixtures.userAdmin(1L);
        when(jwtTokenProvider.parseAndValidate("valid.jwt.token"))
                .thenReturn(new JwtTokenProvider.ParsedJwt(1L, "ana.admin@docurural.edu.co", UserRole.ADMIN));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull().isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getEmail()).isEqualTo("ana.admin@docurural.edu.co");
        assertThat(principal.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void doFilter_withValidToken_authenticationContainsCorrectAuthority() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User editor = TestFixtures.userEditor(2L);
        when(jwtTokenProvider.parseAndValidate("valid.jwt.token"))
                .thenReturn(new JwtTokenProvider.ParsedJwt(2L, "erik.editor@docurural.edu.co", UserRole.EDITOR));
        when(userRepository.findById(2L)).thenReturn(Optional.of(editor));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_EDITOR");
    }

    @Test
    void doFilter_withTokenSurroundedByWhitespace_passesTrimedTokenToProvider() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   trimmed.token   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User admin = TestFixtures.userAdmin(1L);
        when(jwtTokenProvider.parseAndValidate("trimmed.token"))
                .thenReturn(new JwtTokenProvider.ParsedJwt(1L, "ana.admin@docurural.edu.co", UserRole.ADMIN));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        filter.doFilter(request, response, filterChain);

        verify(jwtTokenProvider).parseAndValidate("trimmed.token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilter_whenTokenExpired_clearsContextSetsErrorAttributeAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CredentialsExpiredException exception = new CredentialsExpiredException("Sesión expirada");
        when(jwtTokenProvider.parseAndValidate("expired.jwt.token")).thenThrow(exception);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SecurityConstants.JWT_ERROR_ATTRIBUTE)).isSameAs(exception);
    }

    @Test
    void doFilter_whenTokenInvalid_clearsContextSetsErrorAttributeAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        BadCredentialsException exception = new BadCredentialsException("Token inválido");
        when(jwtTokenProvider.parseAndValidate("invalid.jwt.token")).thenThrow(exception);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SecurityConstants.JWT_ERROR_ATTRIBUTE)).isSameAs(exception);
    }

    @Test
    void doFilter_whenTokenInvalidAndContextHadExistingAuth_existingAuthIsCleared() throws Exception {
        CustomUserPrincipal staleUser = CustomUserPrincipal.fromJwtClaims(99L, "old@docurural.edu.co", UserRole.READER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(staleUser, null, staleUser.getAuthorities()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.parseAndValidate("invalid.jwt.token"))
                .thenThrow(new BadCredentialsException("Token inválido"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_whenUserIsInactive_clearsContextSetsDisabledExceptionAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User inactiveUser = TestFixtures.userInactive(5L);
        when(jwtTokenProvider.parseAndValidate("valid.jwt.token"))
                .thenReturn(new JwtTokenProvider.ParsedJwt(5L, inactiveUser.getEmail(), inactiveUser.getRole()));
        when(userRepository.findById(5L)).thenReturn(Optional.of(inactiveUser));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SecurityConstants.JWT_ERROR_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void doFilter_whenUserDoesNotExistInDb_clearsContextSetsBadCredentialsAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.parseAndValidate("valid.jwt.token"))
                .thenReturn(new JwtTokenProvider.ParsedJwt(99L, "deleted@docurural.edu.co", UserRole.READER));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SecurityConstants.JWT_ERROR_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(BadCredentialsException.class);
    }
}
