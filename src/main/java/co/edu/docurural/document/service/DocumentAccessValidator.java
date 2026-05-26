package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.enums.SensitivityLevel;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.security.CustomUserPrincipal;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Validador para controlar el acceso a documentos según su sensibilidad y el rol del usuario.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentAccessValidator {

    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;

    /**
     * Valida si el usuario actual tiene permisos para acceder al documento solicitado.
     * Si no tiene permisos, registra la acción ACCESS_DENIED en activity_log y lanza una excepción.
     *
     * @param document el documento que se desea acceder
     * @param audit    el contexto de auditoría actual
     * @throws BusinessRuleException si el usuario no tiene permisos
     */
    public void validateAccess(Document document, AuditContext audit) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserPrincipal principal) {
            UserRole role = principal.getRole();
            SensitivityLevel sensitivity = document.getSensitivityLevel();

            if ((sensitivity == SensitivityLevel.RESTRICTED || sensitivity == SensitivityLevel.CONFIDENTIAL)
                    && (role == UserRole.EDITOR || role == UserRole.READER)) {

                String detail = String.format("document_id: %d, sensitivity_level: %s, user_role: %s",
                        document.getId(), sensitivity.name(), role.name());

                activityLogService.record(ActivityAction.ACCESS_DENIED, audit, document.getId(), detail);

                log.warn("Acceso denegado a documento restringido/confidencial. ID: {}, Sensibilidad: {}, Rol Usuario: {}",
                        document.getId(), sensitivity, role);

                throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN,
                        messageResolver.get("document.access.forbidden"));
            }
        }
    }
}

