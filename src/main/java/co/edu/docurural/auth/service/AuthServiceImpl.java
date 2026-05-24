package co.edu.docurural.auth.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.auth.dto.LoginRequestDto;
import co.edu.docurural.auth.dto.LoginResponseDto;
import co.edu.docurural.auth.dto.UserSummaryDto;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.shared.dto.MessageResponseDto;
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
import java.util.Locale;

/**
 * Servicio de autenticación para los endpoints {@code AUTH-01} (login) y
 * {@code AUTH-02} (logout).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Delegar la verificación de credenciales al {@link AuthenticationManager},
 *       dejando que {@link BadCredentialsException} y {@link DisabledException} se
 *       propaguen hasta el {@code GlobalExceptionHandler} que los traduce
 *       a 401 y 403 respectivamente.</li>
 *   <li>Emitir el token JWT con {@link JwtTokenProvider}.</li>
 *   <li>Actualizar {@code last_login} tras un login exitoso.</li>
 *   <li>Registrar las acciones {@code LOGIN} y {@code LOGOUT} en {@code activity_log}
 *       a través de {@link ActivityLogService}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_DETAIL = "Inicio de sesión exitoso";
    private static final String LOGOUT_DETAIL = "Cierre de sesión";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto request, AuditContext audit) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));

        User user = userRepository.findByEmail(normalizedEmail)
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

        log.info("Login exitoso para userId={} email={}", savedUser.getId(), normalizedEmail);

        UserSummaryDto summary = userMapper.toSummary(savedUser);
        return LoginResponseDto.bearer(token, expiresInSeconds, summary);
    }

    @Override
    @Transactional
    public MessageResponseDto logout(AuditContext audit) {
        AuditContext resolvedAudit = requireAuditWithActor(audit);

        User user = userRepository.findById(resolvedAudit.actorUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id " + resolvedAudit.actorUserId()));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userRepository.save(user);

        activityLogService.record(
                ActivityAction.LOGOUT,
                resolvedAudit,
                null,
                LOGOUT_DETAIL);

        log.info("Logout registrado para userId={}", resolvedAudit.actorUserId());
        return new MessageResponseDto(messageResolver.get("auth.logout.success"));
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
