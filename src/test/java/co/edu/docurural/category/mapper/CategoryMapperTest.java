package co.edu.docurural.category.mapper;

import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryMapperTest {

    @Test
    void toCreateResponse_mapsAllFields() {
        Category category = TestFixtures.categoryActive(9L, "Actas", "Actas de reuniones");

        CreateCategoryResponse response = CategoryMapper.toCreateResponse(category, "Categoría creada exitosamente");

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.name()).isEqualTo("Actas");
        assertThat(response.description()).isEqualTo("Actas de reuniones");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(TestFixtures.FIXED_CREATED_AT);
        assertThat(response.message()).isEqualTo("Categoría creada exitosamente");
    }

    @Test
    void toCreateResponse_withNullDescription_mapsDescriptionAsNull() {
        Category category = TestFixtures.categoryActive(1L, "Resoluciones");

        CreateCategoryResponse response = CategoryMapper.toCreateResponse(category, "ok");

        assertThat(response.description()).isNull();
    }

    @Test
    void toCreateResponse_withNullStatus_mapsStatusAsNull() {
        Category category = Category.builder()
                .id(2L)
                .name("Sin estado")
                .createdAt(TestFixtures.FIXED_CREATED_AT)
                .build();

        CreateCategoryResponse response = CategoryMapper.toCreateResponse(category, "ok");

        assertThat(response.status()).isNull();
    }

    @Test
    void toCreateResponse_withNullCategory_throwsNullPointerException() {
        assertThatThrownBy(() -> CategoryMapper.toCreateResponse(null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toCreateResponse_statusIsAlwaysActiveStringForNewCategory() {
        Category category = TestFixtures.categoryActive(5L, "Circulares");

        CreateCategoryResponse response = CategoryMapper.toCreateResponse(category, "msg");

        assertThat(response.status()).isEqualTo(CategoryStatus.ACTIVE.name());
    }
}
