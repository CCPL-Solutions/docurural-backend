package co.edu.docurural.shared.security;

import co.edu.docurural.shared.domain.enums.UserRole;
import co.edu.docurural.shared.security.CustomUserPrincipal;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import co.edu.docurural.shared.security.JwtTokenProvider;
import co.edu.docurural.shared.security.SecurityConstants;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

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

        when(jwtTokenProvider.parseAndValidate("valid.jwt.token"))
                .thenReturn(new JwtTokenProvider.ParsedJwt(1L, "ana.admin@docurural.edu.co", UserRole.ADMIN));

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

        when(jwtTokenProvider.parseAndValidate("valid.jwt.token"))
                .thenReturn(new JwtTokenProvider.ParsedJwt(2L, "erik.editor@docurural.edu.co", UserRole.EDITOR));

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

        when(jwtTokenProvider.parseAndValidate("trimmed.token"))
                .thenReturn(new JwtTokenProvider.ParsedJwt(1L, "ana.admin@docurural.edu.co", UserRole.ADMIN));

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
}
