package co.edu.docurural.category.dto;

/**
 * Aviso de alcance y advertencia de pocos aprobadores que acompañan la respuesta de edición de
 * una categoría (HU-31). Cada uno es {@code null} cuando no aplica.
 */
public record ApprovalNotices(String scopeNotice, String approverWarning) {

    public static ApprovalNotices none() {
        return new ApprovalNotices(null, null);
    }
}
