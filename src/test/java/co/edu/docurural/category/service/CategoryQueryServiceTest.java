package co.edu.docurural.category.service;

import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceTest {

    @Mock
    CategoryRepository categoryRepository;
    @Mock
    MessageResolver messageResolver;
    @InjectMocks
    CategoryQueryServiceImpl categoryQueryService;

    @Test
    void requiresApproval_returnsTrue_whenCategoryRequiresApproval() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(TestFixtures.categoryRequiringApproval(1L, "Actas")));

        assertThat(categoryQueryService.requiresApproval(1L)).isTrue();
    }

    @Test
    void requiresApproval_returnsFalse_whenCategoryDoesNotRequireApproval() {
        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(TestFixtures.categoryActive(2L, "Circulares")));

        assertThat(categoryQueryService.requiresApproval(2L)).isFalse();
    }

    @Test
    void requiresApproval_throwsNotFound_whenCategoryMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageResolver.get("category.not-found", 99L)).thenReturn("Categoría no encontrada con id 99");

        assertThatThrownBy(() -> categoryQueryService.requiresApproval(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoría no encontrada con id 99");
    }
}
