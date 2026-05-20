package co.edu.docurural.category.mapper;

import co.edu.docurural.category.dto.CategoryDetailResponse;
import co.edu.docurural.category.dto.CategoryListResponse;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponse;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.user.domain.entity.User;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

    // ------------------------------------------------------------------
    // toUpdateResponse()
    // ------------------------------------------------------------------

    @Test
    void toUpdateResponse_mapsAllFields() {
        Category category = TestFixtures.categoryActive(9L, "Proyectos e Informes Biotecnología", "Descripción detallada");

        UpdateCategoryResponse response = CategoryMapper.toUpdateResponse(category, "Categoría actualizada exitosamente");

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.name()).isEqualTo("Proyectos e Informes Biotecnología");
        assertThat(response.description()).isEqualTo("Descripción detallada");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("Categoría actualizada exitosamente");
    }

    @Test
    void toUpdateResponse_withNullDescription_mapsDescriptionAsNull() {
        Category category = TestFixtures.categoryActive(3L, "Circulares");

        UpdateCategoryResponse response = CategoryMapper.toUpdateResponse(category, "ok");

        assertThat(response.description()).isNull();
    }

    @Test
    void toUpdateResponse_withNullCategory_throwsNullPointerException() {
        assertThatThrownBy(() -> CategoryMapper.toUpdateResponse(null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }

    // ------------------------------------------------------------------
    // toStatusResponse()
    // ------------------------------------------------------------------

    @Test
    void toStatusResponse_mapsAllFields_whenCategoryActive() {
        Category category = TestFixtures.categoryActive(9L, "Proyectos e Informes Biotecnología");

        UpdateCategoryStatusResponse response = CategoryMapper.toStatusResponse(category, "Categoría desactivada exitosamente");

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.name()).isEqualTo("Proyectos e Informes Biotecnología");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("Categoría desactivada exitosamente");
    }

    @Test
    void toStatusResponse_withInactiveCategory_mapsStatusAsInactive() {
        Category category = TestFixtures.categoryInactive(5L, "Actas");

        UpdateCategoryStatusResponse response = CategoryMapper.toStatusResponse(category, "Categoría desactivada exitosamente");

        assertThat(response.status()).isEqualTo("INACTIVE");
    }

    @Test
    void toStatusResponse_withNullStatus_mapsStatusAsNull() {
        Category category = Category.builder()
                .id(2L)
                .name("Sin estado")
                .createdAt(TestFixtures.FIXED_CREATED_AT)
                .build();

        UpdateCategoryStatusResponse response = CategoryMapper.toStatusResponse(category, "ok");

        assertThat(response.status()).isNull();
    }

    @Test
    void toStatusResponse_withNullCategory_throwsNullPointerException() {
        assertThatThrownBy(() -> CategoryMapper.toStatusResponse(null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }

    // ------------------------------------------------------------------
    // toDetailResponse()
    // ------------------------------------------------------------------

    @Test
    void toDetailResponse_mapsAllFields_withCreatedByUser() {
        User admin = TestFixtures.userAdmin(1L);
        Category category = TestFixtures.categoryActive(1L, "Actas", "Actas de reuniones", admin);

        CategoryDetailResponse response = CategoryMapper.toDetailResponse(category, 23L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Actas");
        assertThat(response.description()).isEqualTo("Actas de reuniones");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.documentCount()).isEqualTo(23);
        assertThat(response.createdAt()).isEqualTo(TestFixtures.FIXED_CREATED_AT);
        assertThat(response.createdBy()).isEqualTo("Ana Admin");
    }

    @Test
    void toDetailResponse_withNullCreatedBy_mapsToSistema() {
        Category category = TestFixtures.categoryActive(2L, "Resoluciones");

        CategoryDetailResponse response = CategoryMapper.toDetailResponse(category, 0L);

        assertThat(response.createdBy()).isEqualTo("Sistema");
    }

    @Test
    void toDetailResponse_withNullStatus_mapsAsNull() {
        Category category = Category.builder()
                .id(3L)
                .name("Sin estado")
                .createdAt(TestFixtures.FIXED_CREATED_AT)
                .build();

        CategoryDetailResponse response = CategoryMapper.toDetailResponse(category, 5L);

        assertThat(response.status()).isNull();
    }

    @Test
    void toDetailResponse_withZeroDocumentCount_mapsToZero() {
        Category category = TestFixtures.categoryActive(4L, "Circulares");

        CategoryDetailResponse response = CategoryMapper.toDetailResponse(category, 0L);

        assertThat(response.documentCount()).isEqualTo(0);
    }

    @Test
    void toDetailResponse_withNullCategory_throwsNullPointerException() {
        assertThatThrownBy(() -> CategoryMapper.toDetailResponse(null, 0L))
                .isInstanceOf(NullPointerException.class);
    }

    // ------------------------------------------------------------------
    // toListResponse()
    // ------------------------------------------------------------------

    @Test
    void toListResponse_emptyList_returnsZeroCounters_andEmptyList() {
        CategoryListResponse response = CategoryMapper.toListResponse(List.of(), Map.of());

        assertThat(response.totalCategories()).isEqualTo(0);
        assertThat(response.activeCategories()).isEqualTo(0);
        assertThat(response.inactiveCategories()).isEqualTo(0);
        assertThat(response.categories()).isEmpty();
    }

    @Test
    void toListResponse_mixedActiveInactive_summarizesCorrectly() {
        Category cat1 = TestFixtures.categoryActive(1L, "Actas");
        Category cat2 = TestFixtures.categoryActive(2L, "Informes");
        Category cat3 = TestFixtures.categoryInactive(3L, "Resoluciones");

        CategoryListResponse response = CategoryMapper.toListResponse(
                List.of(cat1, cat2, cat3),
                Map.of(1L, 10L, 2L, 5L));

        assertThat(response.totalCategories()).isEqualTo(3);
        assertThat(response.activeCategories()).isEqualTo(2);
        assertThat(response.inactiveCategories()).isEqualTo(1);
        assertThat(response.categories().get(0).documentCount()).isEqualTo(10);
        assertThat(response.categories().get(1).documentCount()).isEqualTo(5);
        assertThat(response.categories().get(2).documentCount()).isEqualTo(0);
    }

    @Test
    void toListResponse_missingCountInMap_treatedAsZero() {
        Category category = TestFixtures.categoryActive(9L, "Circulares");

        CategoryListResponse response = CategoryMapper.toListResponse(List.of(category), Map.of());

        assertThat(response.categories().get(0).documentCount()).isEqualTo(0);
    }

    @Test
    void toListResponse_withNullCategories_throwsNullPointerException() {
        assertThatThrownBy(() -> CategoryMapper.toListResponse(null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toListResponse_withNullCounts_throwsNullPointerException() {
        assertThatThrownBy(() -> CategoryMapper.toListResponse(List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
