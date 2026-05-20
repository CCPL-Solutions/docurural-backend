package co.edu.docurural.activitylog.service;

import co.edu.docurural.activitylog.entity.ActivityLog;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.repository.ActivityLogRepository;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
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
 *   <li>Persistir la dirección IP de origen ya resuelta por la capa web dentro
 *       de {@link AuditContext}.</li>
 *   <li>Delegar la fijación de {@code action_timestamp} al {@code @PrePersist} de
 *       la entidad {@link ActivityLog}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final MessageSource messageSource;

    /**
     * Registra una acción de auditoría asociada al usuario {@code userId} y,
     * opcionalmente, a un documento.
     *
     * @param action     tipo de acción (no puede ser {@code null}).
     * @param audit      contexto de auditoría (actor + IP cliente).
     * @param documentId id del documento afectado (puede ser {@code null}).
     * @param detail     descripción libre en español para la trazabilidad.
     * @return entidad {@link ActivityLog} persistida.
     * @throws ResourceNotFoundException si {@code actorUserId} o {@code documentId} no existen.
     */
    @Transactional
    public ActivityLog record(
            ActivityAction action,
            AuditContext audit,
            Long documentId,
            String detail) {

        if (action == null) {
            throw new IllegalArgumentException("action no puede ser null");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit no puede ser null");
        }
        if (audit.actorUserId() == null) {
            throw new IllegalArgumentException("audit.actorUserId no puede ser null");
        }

        User user = userRepository.findById(audit.actorUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        resolve("user.not-found", audit.actorUserId())));

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
                .ipAddress(audit.clientIp())
                .detail(detail)
                .build();

        ActivityLog saved = activityLogRepository.save(entry);
        log.debug("activity_log registrado: id={} action={} userId={} documentId={}",
                saved.getId(), action, audit.actorUserId(), documentId);
        return saved;
    }


    private String resolve(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
