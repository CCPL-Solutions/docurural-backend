package co.edu.docurural.document.repository;

import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Factories de {@link Specification} para búsqueda y filtrado dinámico de documentos
 * (SRC-01 / HU-20, HU-21, HU-22).
 *
 * <p>Cada factory devuelve {@code null} cuando recibe una entrada vacía o {@code null}, de modo
 * que pueden componerse con {@code Specification.where(a).and(b)} sin predicados vacíos.
 */
public final class DocumentSpecifications {

    private DocumentSpecifications() {
    }

    /** Solo documentos con {@code status = ACTIVE}. */
    public static Specification<Document> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), DocumentStatus.ACTIVE);
    }

    /**
     * Filtra por id de categoría exacto.
     *
     * @return {@code null} si {@code categoryId} es {@code null}
     */
    public static Specification<Document> categoryIdEquals(Long categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * Búsqueda parcial ILIKE (case-insensitive) sobre {@code responsible_area}.
     *
     * @return {@code null} si {@code area} es {@code null} o blank
     */
    public static Specification<Document> responsibleAreaContains(String area) {
        if (area == null || area.isBlank()) return null;
        String pattern = "%" + escapeLikeWildcards(area.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("responsibleArea")), pattern);
    }

    /**
     * Filtra documentos cuya {@code document_date} sea {@code >= dateFrom}.
     *
     * @return {@code null} si {@code dateFrom} es {@code null}
     */
    public static Specification<Document> documentDateGte(LocalDate dateFrom) {
        if (dateFrom == null) return null;
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("documentDate"), dateFrom);
    }

    /**
     * Filtra documentos cuya {@code document_date} sea {@code <= dateTo}.
     *
     * @return {@code null} si {@code dateTo} es {@code null}
     */
    public static Specification<Document> documentDateLte(LocalDate dateTo) {
        if (dateTo == null) return null;
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("documentDate"), dateTo);
    }

    /**
     * Filtra por el id del usuario que cargó el documento.
     *
     * @return {@code null} si {@code userId} es {@code null}
     */
    public static Specification<Document> uploadedByEquals(Long userId) {
        if (userId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("uploadedBy").get("id"), userId);
    }

    /**
     * Búsqueda ILIKE case-insensitive sobre {@code title}, {@code description} y
     * {@code original_file_name} combinados con OR.
     *
     * @return {@code null} si {@code q} es {@code null} o blank
     */
    public static Specification<Document> matchesText(String q) {
        if (q == null || q.isBlank()) return null;
        String pattern = "%" + escapeLikeWildcards(q.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("originalFileName")), pattern)
        );
    }

    private static String escapeLikeWildcards(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
