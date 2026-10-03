package co.edu.docurural.document.enums;

/**
 * Estado del documento en el flujo de aprobación (RF-07). Es independiente de
 * {@link DocumentStatus}, que solo indica si el documento está vigente o eliminado.
 */
public enum DocumentWorkflowStatus {
    NOT_REQUIRED,
    DRAFT,
    IN_REVIEW,
    APPROVED,
    ARCHIVED;

    /** Estado con el que nace un documento según su categoría requiera aprobación (HU-33). */
    public static DocumentWorkflowStatus initialFor(boolean requiresApproval) {
        return requiresApproval ? DRAFT : NOT_REQUIRED;
    }
}
