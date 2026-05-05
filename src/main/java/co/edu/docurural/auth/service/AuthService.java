package co.edu.docurural.auth.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.auth.dto.LoginRequest;
import co.edu.docurural.auth.dto.LoginResponse;
import co.edu.docurural.auth.dto.UserSummary;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.domain.entity.User;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.shared.dto.MessageResponse;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.security.JwtProperties;
import co.edu.docurural.shared.security.JwtTokenProvider;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final MessageResolver messageResolver;

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
     * @param request credenciales del usuario (email + password) ya validadas por Bean Validation.
     * @param audit   contexto de auditoría resuelto en la capa web.
     * @return {@link LoginResponse} listo para serializar como body del endpoint.
     */
    @Transactional
    public LoginResponse login(LoginRequest request, AuditContext audit) {
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
                requireAudit(audit).withActorUserId(savedUser.getId()),
                null,
                LOGIN_DETAIL);

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
     * @param audit contexto de auditoría resuelto en la capa web.
     * @return {@link MessageResponse} con el mensaje "Sesión cerrada exitosamente".
     */
    @Transactional
    public MessageResponse logout(AuditContext audit) {
        AuditContext resolvedAudit = requireAuditWithActor(audit);

        activityLogService.record(
                ActivityAction.LOGOUT,
                resolvedAudit,
                null,
                LOGOUT_DETAIL);

        log.info("Logout registrado para userId={}", resolvedAudit.actorUserId());
        return new MessageResponse(messageResolver.get("auth.logout.success"));
    }

    private AuditContext requireAudit(AuditContext audit) {
        if (audit == null) {
            throw new IllegalArgumentException("audit no puede ser null");
        }
        return audit;
    }

    private AuditContext requireAuditWithActor(AuditContext audit) {
        AuditContext resolvedAudit = requireAudit(audit);
        if (resolvedAudit.actorUserId() == null) {
            throw new IllegalArgumentException("audit.actorUserId no puede ser null");
        }
        return resolvedAudit;
    }
}
