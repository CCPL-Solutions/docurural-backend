package co.edu.docurural.category.mapper;

import co.edu.docurural.category.dto.CategoryDetailResponse;
import co.edu.docurural.category.dto.CategoryListResponse;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponse;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mapper(componentModel = "spring")
public abstract class CategoryMapper {

    @BeforeMapping
    protected void requireNonNull(Category category) {
        Objects.requireNonNull(category, "category no puede ser null");
    }

    @Mapping(target = "status",
            expression = "java(category.getStatus() != null ? category.getStatus().name() : null)")
    @Mapping(target = "createdBy",
            expression = "java(category.getCreatedBy() != null ? category.getCreatedBy().getFullName() : \"Sistema\")")
    public abstract CategoryDetailResponse toDetailResponse(Category category, long documentCount);

    @Mapping(target = "status",
            expression = "java(category.getStatus() != null ? category.getStatus().name() : null)")
    public abstract CreateCategoryResponse toCreateResponse(Category category, String message);

    @Mapping(target = "status",
            expression = "java(category.getStatus() != null ? category.getStatus().name() : null)")
    public abstract UpdateCategoryResponse toUpdateResponse(Category category, String message);

    @Mapping(target = "status",
            expression = "java(category.getStatus() != null ? category.getStatus().name() : null)")
    public abstract UpdateCategoryStatusResponse toStatusResponse(Category category, String message);

    public CategoryListResponse toListResponse(List<Category> categories, Map<Long, Long> countsByCategoryId) {
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
}
