package co.edu.docurural.shared.audit;

/**
 * Contexto mínimo de auditoría desacoplado de Servlet para capa de servicio.
 */
public record AuditContext(Long actorUserId, String clientIp) {

    public AuditContext withActorUserId(Long actorUserId) {
        return new AuditContext(actorUserId, clientIp);
    }
}

