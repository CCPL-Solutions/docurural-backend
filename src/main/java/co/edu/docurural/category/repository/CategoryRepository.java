package co.edu.docurural.category.repository;

import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.repository.projection.CategoryNameView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
