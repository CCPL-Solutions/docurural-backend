package co.edu.docurural.activitylog.service;

import co.edu.docurural.activitylog.entity.ActivityLog;
import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.repository.ActivityLogRepository;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de auditoría.
 *
 * <p>Usa {@code REQUIRES_NEW} para abrir siempre una transacción independiente:
 * si el registro falla (constraint, usuario inexistente, etc.) solo se revierte
 * el log — la transacción del llamador sigue intacta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final MessageSource messageSource;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ActivityAction action, AuditContext audit, Long documentId, String detail) {
        if (action == null) {
            throw new IllegalArgumentException("action no puede ser null");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit no puede ser null");
        }
        if (audit.actorUserId() == null) {
            throw new IllegalArgumentException("audit.actorUserId no puede ser null");
        }

        try {
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
        } catch (Exception ex) {
            log.error("[AUDIT_FAILED] No se pudo registrar auditoría [action={} actorId={}]",
                    action, audit.actorUserId(), ex);
        }
    }

    private String resolve(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
