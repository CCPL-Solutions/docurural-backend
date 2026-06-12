package co.edu.docurural.category.service;

import co.edu.docurural.category.dto.CategoryDetailResponseDto;
import co.edu.docurural.category.dto.CategoryListResponseDto;
import co.edu.docurural.category.dto.CreateCategoryRequestDto;
import co.edu.docurural.category.dto.CreateCategoryResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponseDto;
import co.edu.docurural.category.repository.projection.CategoryNameView;
import co.edu.docurural.shared.audit.AuditContext;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface CategoryService {

    CategoryListResponseDto list(String sortBy, String sortDir);

    CategoryDetailResponseDto findById(Long id);

    CreateCategoryResponseDto create(CreateCategoryRequestDto request, AuditContext audit);

    UpdateCategoryResponseDto update(Long id, UpdateCategoryRequestDto request, AuditContext audit);

    UpdateCategoryStatusResponseDto changeStatus(Long id, UpdateCategoryStatusRequestDto request, AuditContext audit);

    /**
     * Proyección ligera id/nombre de todas las categorías, ordenadas por {@code sort}.
     * Usada por el módulo {@code dashboard} sin necesidad de acceder al repositorio interno.
     */
    List<CategoryNameView> findAllCategoryNames(Sort sort);
}
