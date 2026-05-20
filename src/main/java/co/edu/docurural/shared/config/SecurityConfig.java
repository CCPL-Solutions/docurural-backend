package co.edu.docurural.shared.config;

import co.edu.docurural.shared.dto.ApiErrorResponse;
import co.edu.docurural.shared.security.CustomUserDetailsService;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import co.edu.docurural.shared.security.JwtProperties;
import co.edu.docurural.shared.security.SecurityConstants;
import co.edu.docurural.shared.util.MessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Configuración global de Spring Security para DocuRural.
 *
 * <p>Política:
 * <ul>
 *   <li>Stateless: no se mantiene sesión HTTP; cada petición se autentica con JWT.</li>
 *   <li>CSRF deshabilitado (API REST sin cookies de sesión).</li>
 *   <li>CORS habilitado vía el {@code CorsConfigurationSource} declarado en {@code CorsConfig}.</li>
 *   <li>Método de acceso por rol habilitado con {@link EnableMethodSecurity}
 *       para poder usar {@code @PreAuthorize("hasRole('ADMIN')")} en los controllers.</li>
 * </ul>
 *
 * <p>Las rutas se evalúan con el {@code context-path} de la aplicación ya resuelto
 * por Spring, por lo que {@code /auth/login} aquí corresponde al endpoint público
 * {@code /api/auth/login} en la URL externa.
 *
 * <p>Los errores de autenticación y autorización que ocurren <em>antes</em> de entrar
 * al controller (filtro JWT, rechazos del {@code FilterSecurityInterceptor}) se
 * serializan aquí con el mismo {@link ApiErrorResponse} que usa el
 * {@code GlobalExceptionHandler} para los errores capturados dentro del controller.
 * Así el cliente siempre recibe la misma estructura.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;
    private final MessageResolver messageResolver;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoAuthenticationProvider) {
        return daoAuthenticationProvider::authenticate;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/auth/login").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Escribe un JSON 401 con el mensaje adecuado según la causa detectada por el
     * filtro JWT (ver {@link SecurityConstants#JWT_ERROR_ATTRIBUTE}):
     * <ul>
     *   <li>Sin atributo o {@code CredentialsExpiredException} -> mensaje "sesión expirada".</li>
     *   <li>{@code BadCredentialsException} (token malformado/inválido) -> "Token inválido".</li>
     *   <li>Resto de {@code AuthenticationException} -> fallback "sesión expirada".</li>
     * </ul>
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            AuthenticationException cause = resolveCause(request, authException);
            if (cause instanceof DisabledException) {
                writeJsonError(response, HttpStatus.FORBIDDEN,
                        messageResolver.get("auth.login.account-disabled"));
            } else {
                writeJsonError(response, HttpStatus.UNAUTHORIZED, resolveUnauthorizedMessage(cause));
            }
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeJsonError(response, HttpStatus.FORBIDDEN, messageResolver.get("auth.access-denied"));
    }

    private AuthenticationException resolveCause(HttpServletRequest request, AuthenticationException fallback) {
        Object attr = request.getAttribute(SecurityConstants.JWT_ERROR_ATTRIBUTE);
        if (attr instanceof AuthenticationException filterCause) {
            return filterCause;
        }
        return fallback;
    }

    private String resolveUnauthorizedMessage(AuthenticationException cause) {
        if (cause instanceof CredentialsExpiredException) {
            return messageResolver.get("auth.session.expired");
        }
        if (cause instanceof BadCredentialsException) {
            return messageResolver.get("auth.token.invalid");
        }
        return messageResolver.get("auth.session.expired");
    }

    private void writeJsonError(
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
