package co.edu.docurural.shared.audit;

import co.edu.docurural.shared.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Construye el contexto de auditoría en la capa web para no acoplar servicios a Servlet.
 */
@Component
public class AuditContextResolver {

    public AuditContext resolve(HttpServletRequest request) {
        return new AuditContext(currentUserId().orElse(null), ClientIpResolver.from(request));
    }

    private Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal customPrincipal) {
            return Optional.of(customPrincipal.getId());
        }

        return Optional.empty();
    }
}

