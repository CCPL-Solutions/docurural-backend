package co.edu.docurural.category.service;

import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryQueryServiceImpl implements CategoryQueryService {

    private final CategoryRepository categoryRepository;
    private final MessageResolver messageResolver;

    @Override
    @Transactional(readOnly = true)
    public boolean requiresApproval(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(Category::isRequiresApproval)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("category.not-found", categoryId)));
    }
}
