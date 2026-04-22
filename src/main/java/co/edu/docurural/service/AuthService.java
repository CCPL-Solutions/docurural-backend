package co.edu.docurural.service;

import co.edu.docurural.config.security.CustomUserPrincipal;
import co.edu.docurural.config.security.JwtProperties;
import co.edu.docurural.config.security.JwtTokenProvider;
import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.enums.ActivityAction;
import co.edu.docurural.domain.repository.UserRepository;
import co.edu.docurural.web.dto.auth.LoginRequest;
import co.edu.docurural.web.dto.auth.LoginResponse;
import co.edu.docurural.web.dto.auth.UserSummary;
import co.edu.docurural.web.dto.common.MessageResponse;
import co.edu.docurural.web.exception.ResourceNotFoundException;
import co.edu.docurural.web.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de autenticación para los endpoints {@code AUTH-01} (login) y
 * {@code AUTH-02} (logout).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Delegar la verificación de credenciales al {@link AuthenticationManager},
 *       dejando que {@link BadCredentialsException} y {@link DisabledException} se
 *       propaguen hasta el {@code GlobalExceptionHandler} (Fase 7) que los traduce
 *       a 401 y 403 respectivamente con los mensajes en español del contrato.</li>
 *   <li>Emitir el token JWT con {@link JwtTokenProvider}.</li>
 *   <li>Actualizar {@code last_login} tras un login exitoso.</li>
 *   <li>Registrar las acciones {@code LOGIN} y {@code LOGOUT} en {@code activity_log}
 *       a través de {@link ActivityLogService}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String LOGIN_DETAIL = "Inicio de sesión exitoso";
    private static final String LOGOUT_DETAIL = "Cierre de sesión";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final MessageSource messageSource;

    /**
     * Autentica al usuario con email + password y emite un token JWT.
     *
     * <p>Comportamiento ante errores:
     * <ul>
     *   <li>Credenciales inválidas -> propaga {@link BadCredentialsException}
     *       (401 "Correo o contraseña incorrectos").</li>
     *   <li>Cuenta {@code INACTIVE} -> propaga {@link DisabledException}
     *       (403 "Su cuenta ha sido desactivada...").</li>
     * </ul>
     *
     * <p>Tras un login exitoso:
     * <ol>
     *   <li>Se actualiza {@code user.lastLogin} a {@code LocalDateTime.now()}.</li>
     *   <li>Se registra la acción {@link ActivityAction#LOGIN} en {@code activity_log}.</li>
     *   <li>Se retorna el {@link LoginResponse} con el token, su tipo
     *       ({@code "Bearer"}), la duración en segundos y el resumen del usuario.</li>
     * </ol>
     *
     * @param request     credenciales del usuario (email + password) ya validadas por Bean Validation.
     * @param httpRequest petición HTTP usada para resolver la IP de origen del registro de auditoría.
     * @return {@link LoginResponse} listo para serializar como body del endpoint.
     */
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // Deja que BadCredentialsException y DisabledException se propaguen;
        // el GlobalExceptionHandler (Fase 7) se encargará de traducirlas a 401 / 403.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado con email " + request.email()));

        user.setLastLogin(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(savedUser);
        long expiresInSeconds = jwtProperties.getExpirationMs() / 1000L;

        activityLogService.record(
                ActivityAction.LOGIN,
                savedUser.getId(),
                null,
                LOGIN_DETAIL,
                httpRequest);

        log.info("Login exitoso para userId={} email={}", savedUser.getId(), savedUser.getEmail());

        UserSummary summary = UserMapper.toSummary(savedUser);
        return LoginResponse.bearer(token, expiresInSeconds, summary);
    }

    /**
     * Registra el cierre de sesión del usuario autenticado actual en
     * {@code activity_log} y retorna el mensaje de confirmación del contrato
     * {@code AUTH-02}.
     *
     * <p>Al ser JWT stateless, la invalidación del token se gestiona en el cliente;
     * este método solo persiste el evento {@link ActivityAction#LOGOUT} para fines
     * de auditoría.
     *
     * @param httpRequest petición HTTP usada para resolver la IP de origen del registro.
     * @return {@link MessageResponse} con el mensaje "Sesión cerrada exitosamente".
     * @throws IllegalStateException si no hay un usuario autenticado en el contexto
     *                               (lo cual no debería ocurrir porque el endpoint
     *                               exige autenticación).
     */
    @Transactional
    public MessageResponse logout(HttpServletRequest httpRequest) {
        CustomUserPrincipal principal = requireCurrentPrincipal();

        activityLogService.record(
                ActivityAction.LOGOUT,
                principal.getId(),
                null,
                LOGOUT_DETAIL,
                httpRequest);

        log.info("Logout registrado para userId={} email={}", principal.getId(), principal.getEmail());
        return new MessageResponse(resolve("auth.logout.success"));
    }

    private CustomUserPrincipal requireCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "No hay un usuario autenticado en el contexto de seguridad");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
            throw new IllegalStateException(
                    "El principal no es una instancia de CustomUserPrincipal: "
                            + (principal == null ? "null" : principal.getClass().getName()));
        }
        return customPrincipal;
    }

    private String resolve(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
