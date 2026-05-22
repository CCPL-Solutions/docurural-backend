package co.edu.docurural.category.repository.projection;

/**
 * Proyección de solo lectura de {@code Category} con los campos mínimos requeridos
 * por el dashboard (DSH-01). Evita el JOIN a {@code users} que arrastra
 * {@link co.edu.docurural.category.repository.CategoryRepository#findAll(org.springframework.data.domain.Sort)}.
 */
public interface CategoryNameView {

    Long getId();

    String getName();
}
