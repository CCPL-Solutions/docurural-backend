package co.edu.docurural.activitylog.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.shared.audit.AuditContext;

/**
 * Contrato público del servicio de auditoría.
 *
 * <p>La implementación ejecuta en una transacción independiente ({@code REQUIRES_NEW})
 * para que un fallo al registrar el log nunca revierta la operación de negocio del llamador.
 */
public interface ActivityLogService {

    /**
     * Registra una acción de auditoría asociada al usuario indicado en {@code audit} y,
     * opcionalmente, a un documento.
     *
     * @param action     tipo de acción (no puede ser {@code null}).
     * @param audit      contexto de auditoría (actor + IP cliente).
     * @param documentId id del documento afectado (puede ser {@code null}).
     * @param detail     descripción libre en español para la trazabilidad.
     * @throws IllegalArgumentException si {@code action}, {@code audit} o
     *                                  {@code audit.actorUserId()} son {@code null}
     *                                  (errores de programación que deben propagarse).
     */
    void record(ActivityAction action, AuditContext audit, Long documentId, String detail);
}
