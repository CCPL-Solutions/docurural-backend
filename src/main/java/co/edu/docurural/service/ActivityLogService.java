package co.edu.docurural.service;

import co.edu.docurural.domain.entity.ActivityLog;
import co.edu.docurural.domain.entity.Document;
import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.enums.ActivityAction;
import co.edu.docurural.domain.repository.ActivityLogRepository;
import co.edu.docurural.domain.repository.DocumentRepository;
import co.edu.docurural.domain.repository.UserRepository;
import co.edu.docurural.web.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que persiste los registros de auditoría ({@code activity_log}) para las
 * acciones relevantes del sistema ({@code LOGIN}, {@code LOGOUT}, {@code CREATE_USER},
 * etc.).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Resolver las relaciones {@link User} y (opcionalmente) {@link Document} a
 *       partir de sus ids.</li>
 *   <li>Determinar la dirección IP de origen respetando el header
 *       {@code X-Forwarded-For} cuando la aplicación corre detrás de un proxy
 *       reverso (Nginx en producción), con {@code request.getRemoteAddr()} como
 *       fallback.</li>
 *   <li>Delegar la fijación de {@code action_timestamp} al {@code @PrePersist} de
 *       la entidad {@link ActivityLog}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final int IP_ADDRESS_MAX_LENGTH = 45;

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final MessageSource messageSource;

    /**
     * Registra una acción de auditoría asociada al usuario {@code userId} y,
     * opcionalmente, a un documento.
     *
     * @param action     tipo de acción (no puede ser {@code null}).
     * @param userId     id del usuario que ejecuta la acción (obligatorio).
     * @param documentId id del documento afectado (puede ser {@code null}).
     * @param detail     descripción libre en español para la trazabilidad.
     * @param request    petición HTTP usada para resolver la IP de origen; puede
     *                   ser {@code null} en escenarios de test o tareas en segundo plano.
     * @return entidad {@link ActivityLog} persistida.
     * @throws ResourceNotFoundException si {@code userId} o {@code documentId} no existen.
     */
    @Transactional
    public ActivityLog record(
            ActivityAction action,
            Long userId,
            Long documentId,
            String detail,
            HttpServletRequest request) {

        if (action == null) {
            throw new IllegalArgumentException("action no puede ser null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        resolve("user.not-found", userId)));

        Document document = null;
        if (documentId != null) {
            document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            resolve("document.not-found", documentId)));
        }

        ActivityLog entry = ActivityLog.builder()
                .user(user)
                .action(action)
                .document(document)
                .ipAddress(resolveClientIp(request))
                .detail(detail)
                .build();

        ActivityLog saved = activityLogRepository.save(entry);
        log.debug("activity_log registrado: id={} action={} userId={} documentId={}",
                saved.getId(), action, userId, documentId);
        return saved;
    }

    /**
     * Resuelve la IP del cliente dando prioridad al header {@code X-Forwarded-For}
     * (si contiene una lista separada por comas, toma el primer valor) y cae a
     * {@link HttpServletRequest#getRemoteAddr()} como fallback.
     *
     * <p>Trunca el valor resultante a {@value #IP_ADDRESS_MAX_LENGTH} caracteres
     * para respetar la restricción de la columna {@code ip_address VARCHAR(45)}
     * (longitud suficiente para IPv6).
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIdx = forwardedFor.indexOf(',');
            String first = (commaIdx >= 0 ? forwardedFor.substring(0, commaIdx) : forwardedFor).trim();
            if (!first.isEmpty()) {
                return truncate(first);
            }
        }

        String remote = request.getRemoteAddr();
        return remote == null ? null : truncate(remote);
    }

    private String truncate(String value) {
        if (value.length() <= IP_ADDRESS_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, IP_ADDRESS_MAX_LENGTH);
    }

    private String resolve(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
