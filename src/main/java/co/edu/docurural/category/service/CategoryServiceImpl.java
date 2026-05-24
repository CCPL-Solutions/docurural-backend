package co.edu.docurural.category.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.dto.CategoryDetailResponse;
import co.edu.docurural.category.dto.CategoryListResponse;
import co.edu.docurural.category.dto.CreateCategoryRequest;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryRequest;
import co.edu.docurural.category.dto.UpdateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequest;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponse;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.mapper.CategoryMapper;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.category.repository.projection.CategoryCountView;
import co.edu.docurural.category.repository.projection.CategoryNameView;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.FieldUpdater;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.shared.util.SortingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de categorías documentales (CAT-01..CAT-05 / HU-16..HU-19).
 *
 * <p>Los conteos de documentos activos se obtienen directamente del repositorio
 * de categorías mediante SQL nativo, eliminando la dependencia cruzada con el
 * módulo {@code document}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private static final String DEFAULT_SORT_BY = "name";
    private static final String DEFAULT_SORT_DIR = "asc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt");

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;
    private final SortingValidator sortingValidator;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public CategoryListResponse list(String sortBy, String sortDir) {
        Sort sort = sortingValidator.resolveSort(
                sortBy, sortDir,
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_BY, DEFAULT_SORT_DIR,
                "category.sort.unsupported-field", "category.sort.unsupported-direction");

        List<Category> categories = categoryRepository.findAll(sort);
        Map<Long, Long> counts = buildCountsMap();

        log.debug("Listado de categorías: total={} sortBy={} sortDir={}",
                categories.size(), sortBy, sortDir);
        return categoryMapper.toListResponse(categories, counts);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDetailResponse findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("category.not-found", id)));

        long count = categoryRepository.countActiveDocumentsByCategoryId(id);

        return categoryMapper.toDetailResponse(category, count);
    }

    @Override
    @Transactional
    public CreateCategoryResponse create(CreateCategoryRequest request, AuditContext audit) {
        Long adminId = requireActorUserId(audit);

        if (categoryRepository.existsByName(request.name())) {
            throw new ConflictException(messageResolver.get("category.name.already-registered"));
        }

        Category newCategory = Category.builder()
                .name(request.name())
                .description(request.description())
                .createdBy(userRepository.getReferenceById(adminId))
                .build();

        Category saved = categoryRepository.save(newCategory);

        activityLogService.record(
                ActivityAction.CREATE_CATEGORY,
                audit,
                null,
                "Categoria creada: " + saved.getName());

        log.info("Categoria creada: id={} name='{}' por adminId={}", saved.getId(), saved.getName(), adminId);

        return categoryMapper.toCreateResponse(saved, messageResolver.get("category.created.success"));
    }

    @Override
    @Transactional
    public UpdateCategoryResponse update(Long id, UpdateCategoryRequest request, AuditContext audit) {
        requireActorUserId(audit);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("category.not-found", id)));

        category.assertEditable(messageResolver.get("category.inactive.cannot-edit"));

        boolean nameChanged = !request.name().equals(category.getName());
        if (nameChanged && categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException(messageResolver.get("category.name.already-registered"));
        }

        List<String> modifiedFields = applyUpdates(category, request, nameChanged);

        Category updated = categoryRepository.save(category);

        activityLogService.record(
                ActivityAction.EDIT_CATEGORY,
                audit,
                null,
                "Campos modificados: " + modifiedFields);

        log.info("Categoria actualizada: id={} modifiedFields={} por adminId={}", updated.getId(), modifiedFields, audit.actorUserId());

        return categoryMapper.toUpdateResponse(updated, messageResolver.get("category.updated.success"));
    }

    @Override
    @Transactional
    public UpdateCategoryStatusResponse changeStatus(Long id, UpdateCategoryStatusRequest request, AuditContext audit) {
        Long adminId = requireActorUserId(audit);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("category.not-found", id)));

        CategoryStatus newStatus = request.status();

        if (category.getStatus() == newStatus) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("category.status.duplicate"));
        }

        category.setStatus(newStatus);
        Category updated = categoryRepository.save(category);

        String message = newStatus == CategoryStatus.ACTIVE
                ? messageResolver.get("category.activated.success")
                : messageResolver.get("category.deactivated.success");

        activityLogService.record(
                ActivityAction.DEACTIVATE_CATEGORY,
                audit,
                null,
                "Estado cambiado a " + newStatus.name());

        log.info("Estado de categoría actualizado: id={} newStatus={} por adminId={}", updated.getId(), newStatus, adminId);

        return categoryMapper.toStatusResponse(updated, message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryNameView> findAllCategoryNames(Sort sort) {
        return categoryRepository.findAllBy(sort);
    }

    private Map<Long, Long> buildCountsMap() {
        return categoryRepository.countActiveDocumentsByCategory().stream()
                .collect(Collectors.toMap(CategoryCountView::getCategoryId, CategoryCountView::getCount));
    }

    private List<String> applyUpdates(Category category, UpdateCategoryRequest request, boolean nameChanged) {
        List<String> modifiedFields = new ArrayList<>(
                FieldUpdater.of(category)
                        .setIfPresent("description", request.description(),
                                category::getDescription, category::setDescription)
                        .changedFields());

        if (nameChanged) {
            category.setName(request.name());
            modifiedFields.add(0, "name");
        }

        return modifiedFields;
    }

    private Long requireActorUserId(AuditContext audit) {
        if (audit == null) {
            throw new IllegalArgumentException("audit no puede ser null");
        }
        if (audit.actorUserId() == null) {
            throw new IllegalArgumentException("audit.actorUserId no puede ser null");
        }
        return audit.actorUserId();
    }
}
