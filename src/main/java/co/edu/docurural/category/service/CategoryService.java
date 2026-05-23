package co.edu.docurural.category.service;

import co.edu.docurural.category.dto.CategoryDetailResponse;
import co.edu.docurural.category.dto.CategoryListResponse;
import co.edu.docurural.category.dto.CreateCategoryRequest;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryRequest;
import co.edu.docurural.category.dto.UpdateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequest;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponse;
import co.edu.docurural.category.repository.projection.CategoryNameView;
import co.edu.docurural.shared.audit.AuditContext;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface CategoryService {

    CategoryListResponse list(String sortBy, String sortDir);

    CategoryDetailResponse findById(Long id);

    CreateCategoryResponse create(CreateCategoryRequest request, AuditContext audit);

    UpdateCategoryResponse update(Long id, UpdateCategoryRequest request, AuditContext audit);

    UpdateCategoryStatusResponse changeStatus(Long id, UpdateCategoryStatusRequest request, AuditContext audit);

    /**
     * Proyección ligera id/nombre de todas las categorías, ordenadas por {@code sort}.
     * Usada por el módulo {@code dashboard} sin necesidad de acceder al repositorio interno.
     */
    List<CategoryNameView> findAllCategoryNames(Sort sort);
}
