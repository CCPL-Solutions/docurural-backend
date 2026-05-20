package co.edu.docurural.category.repository;

import co.edu.docurural.category.entity.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"createdBy"})
    List<Category> findAll(Sort sort);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
