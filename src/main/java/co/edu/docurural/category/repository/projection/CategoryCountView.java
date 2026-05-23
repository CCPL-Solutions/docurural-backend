package co.edu.docurural.category.repository.projection;

/**
 * Proyección ligera para conteos de documentos activos por categoría.
 * Permite a {@code CategoryRepository} obtener los conteos sin depender
 * de clases del módulo {@code document}.
 */
public interface CategoryCountView {

    Long getCategoryId();

    Long getCount();
}
