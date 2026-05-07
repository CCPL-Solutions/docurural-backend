package co.edu.docurural.category.mapper;

import co.edu.docurural.category.dto.CategoryDetailResponse;
import co.edu.docurural.category.dto.CategoryListResponse;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponse;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mapper estático entre la entidad {@link Category} y los DTOs públicos del módulo.
 */
public final class CategoryMapper {

    private static final String CREATED_BY_FALLBACK = "Sistema";

    private CategoryMapper() {
    }

    /**
     * Construye el detalle de una categoría para lectura (CAT-01 items y CAT-02 / HU-19).
     *
     * <p>Cuando {@code createdBy} es {@code null} (categorías sembradas por Flyway)
     * se usa el literal {@value #CREATED_BY_FALLBACK}.
     */
    public static CategoryDetailResponse toDetailResponse(Category category, long documentCount) {
        Objects.requireNonNull(category, "category no puede ser null");
        return new CategoryDetailResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus() != null ? category.getStatus().name() : null,
                documentCount,
                category.getCreatedAt(),
                category.getCreatedBy() != null ? category.getCreatedBy().getFullName() : CREATED_BY_FALLBACK);
    }

    /**
     * Empaqueta el listado completo con los contadores agregados (CAT-01 / HU-19).
     */
    public static CategoryListResponse toListResponse(List<Category> categories, Map<Long, Long> countsByCategoryId) {
        Objects.requireNonNull(categories, "categories no puede ser null");
        Objects.requireNonNull(countsByCategoryId, "countsByCategoryId no puede ser null");

        List<CategoryDetailResponse> items = categories.stream()
                .map(c -> toDetailResponse(c, countsByCategoryId.getOrDefault(c.getId(), 0L)))
                .toList();

        int active = (int) categories.stream()
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE).count();
        int inactive = (int) categories.stream()
                .filter(c -> c.getStatus() == CategoryStatus.INACTIVE).count();

        return new CategoryListResponse(items.size(), active, inactive, items);
    }

    /**
     * Construye la respuesta de creación de categoría (CAT-03 / HU-16).
     */
    public static CreateCategoryResponse toCreateResponse(Category category, String message) {
        Objects.requireNonNull(category, "category no puede ser null");
        return new CreateCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus() != null ? category.getStatus().name() : null,
                category.getCreatedAt(),
                message
        );
    }

    /**
     * Construye la respuesta de edición de categoría (CAT-04 / HU-17).
     */
    public static UpdateCategoryResponse toUpdateResponse(Category category, String message) {
        Objects.requireNonNull(category, "category no puede ser null");
        return new UpdateCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus() != null ? category.getStatus().name() : null,
                message
        );
    }

    /**
     * Construye la respuesta de cambio de estado de categoría (CAT-05 / HU-18).
     */
    public static UpdateCategoryStatusResponse toStatusResponse(Category category, String message) {
        Objects.requireNonNull(category, "category no puede ser null");
        return new UpdateCategoryStatusResponse(
                category.getId(),
                category.getName(),
                category.getStatus() != null ? category.getStatus().name() : null,
                message
        );
    }
}
