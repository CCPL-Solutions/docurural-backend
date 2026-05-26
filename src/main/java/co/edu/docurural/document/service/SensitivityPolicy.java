package co.edu.docurural.document.service;

import co.edu.docurural.shared.enums.SensitivityLevel;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reglas de negocio para la asignación de niveles de sensibilidad documental (HU-28).
 */
@Component
@RequiredArgsConstructor
public class SensitivityPolicy {

    private final MessageResolver messageResolver;

    /**
     * Valida que el nivel solicitado para un documento no sea inferior al mínimo de su categoría.
     *
     * @throws BusinessRuleException (INVALID_ARGUMENT / HTTP 400) si el nivel está por debajo.
     */
    public void validateRequestedLevel(SensitivityLevel requested, SensitivityLevel categoryDefault) {
        if (!requested.isAtLeast(categoryDefault)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.sensitivity.below-category-default"));
        }
    }

    /**
     * Valida que el rol del actor tenga permiso para asignar el nivel indicado.
     * Los usuarios con rol EDITOR no pueden asignar RESTRICTED ni CONFIDENTIAL.
     *
     * @throws BusinessRuleException (FORBIDDEN / HTTP 403) si el rol no lo permite.
     */
    public void validateRolePermission(UserRole role, SensitivityLevel requested) {
        if (role == UserRole.EDITOR && (requested == SensitivityLevel.RESTRICTED || requested == SensitivityLevel.CONFIDENTIAL)) {
            throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN,
                    messageResolver.get("document.sensitivity.confidential.forbidden-for-editor"));
        }
    }
}
