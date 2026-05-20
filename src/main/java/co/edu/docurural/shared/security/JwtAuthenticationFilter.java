package co.edu.docurural.shared.security;

import co.edu.docurural.shared.config.SecurityConfig;
import co.edu.docurural.user.domain.entity.User;
import co.edu.docurural.user.domain.enums.UserStatus;
import co.edu.docurural.user.domain.repository.UserRepository;
import co.edu.docurural.shared.util.MessageResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Filtro que extrae y valida el token JWT del header {@code Authorization: Bearer <token>}
 * y, si es válido, puebla el {@link SecurityContextHolder} con un
 * {@link UsernamePasswordAuthenticationToken} cuyo principal es un
 * {@link CustomUserPrincipal} construido a partir de los claims.
 *
 * <p>Cuando el token es inválido o expirado, el filtro limpia el contexto y deja que
 * la cadena continúe; será el {@code AuthenticationEntryPoint} configurado en
 * {@link SecurityConfig} quien emita el JSON 401 apropiado. No se escribe la respuesta
 * desde aquí para concentrar el formato de error en un único punto.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final MessageResolver messageResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtTokenProvider.ParsedJwt claims = jwtTokenProvider.parseAndValidate(token);

            User user = userRepository.findById(claims.getUserId())
                    .orElseThrow(() -> new BadCredentialsException("Token inválido"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new DisabledException("Cuenta desactivada");
            }

            if (!Objects.equals(claims.getTokenVersion(), user.getTokenVersion())) {
                throw new CredentialsExpiredException(messageResolver.get("auth.session.expired"));
            }

            CustomUserPrincipal principal = CustomUserPrincipal.fromEntity(user);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            request.setAttribute(SecurityConstants.JWT_ERROR_ATTRIBUTE, ex);
            log.debug("Autenticación JWT fallida: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login")
                || path.startsWith("/api/swagger-ui")
                || path.startsWith("/api/v3/api-docs");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}
