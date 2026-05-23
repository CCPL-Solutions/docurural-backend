package co.edu.docurural.category.repository;

import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.repository.projection.CategoryCountView;
import co.edu.docurural.category.repository.projection.CategoryNameView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"createdBy"})
    List<Category> findAll(Sort sort);

    /**
     * Proyección ligera id/name para el dashboard (DSH-01).
     * No carga {@code createdBy}, evitando el JOIN innecesario a {@code users}.
     */
    List<CategoryNameView> findAllBy(Sort sort);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Conteos de documentos ACTIVE agrupados por categoría.
     * Usa SQL nativo para evitar importar clases del módulo {@code document}.
     */
    @Query(value = "SELECT d.category_id AS categoryId, COUNT(*) AS count FROM documents d WHERE d.status = 'ACTIVE' GROUP BY d.category_id",
           nativeQuery = true)
    List<CategoryCountView> countActiveDocumentsByCategory();

    /**
     * Conteo de documentos ACTIVE para una categoría específica.
     */
    @Query(value = "SELECT COUNT(*) FROM documents d WHERE d.category_id = :categoryId AND d.status = 'ACTIVE'",
           nativeQuery = true)
    long countActiveDocumentsByCategoryId(@Param("categoryId") Long categoryId);
}
