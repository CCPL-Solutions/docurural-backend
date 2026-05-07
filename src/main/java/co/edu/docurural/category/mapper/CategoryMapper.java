package co.edu.docurural.category.mapper;

import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryResponse;
import co.edu.docurural.category.entity.Category;

import java.util.Objects;

/**
 * Mapper estático entre la entidad {@link Category} y los DTOs públicos del módulo.
 */
public final class CategoryMapper {

    private CategoryMapper() {
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
}
