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
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.repository.projection.CategoryDocumentCount;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de gestión de categorías documentales (CAT-01..CAT-05 / HU-16..HU-19).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private static final String DEFAULT_SORT_BY = "name";
    private static final String DEFAULT_SORT_DIR = "asc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt");

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;

    /**
     * Retorna el listado completo de categorías (ACTIVE + INACTIVE) ordenado según los parámetros.
     *
     * @param sortBy  campo de ordenamiento: {@code name} o {@code createdAt} (default {@code name}).
     * @param sortDir dirección {@code asc} o {@code desc} (default {@code asc}, case-insensitive).
     * @throws BusinessRuleException {@code 400} si {@code sortBy} o {@code sortDir} son inválidos.
     */
    @Transactional(readOnly = true)
    public CategoryListResponse list(String sortBy, String sortDir) {
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? DEFAULT_SORT_BY : sortBy;
        String resolvedSortDir = (sortDir == null || sortDir.isBlank()) ? DEFAULT_SORT_DIR : sortDir;

        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortBy)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("category.sort.unsupported-field", resolvedSortBy));
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(resolvedSortDir.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("category.sort.unsupported-direction", resolvedSortDir));
        }

        List<Category> categories = categoryRepository.findAll(Sort.by(direction, resolvedSortBy));
        Map<Long, Long> counts = documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE).stream()
                .collect(Collectors.toMap(CategoryDocumentCount::getCategoryId, CategoryDocumentCount::getCount));

        log.debug("Listado de categorías: total={} sortBy={} sortDir={}",
                categories.size(), resolvedSortBy, direction);
        return CategoryMapper.toListResponse(categories, counts);
    }

    /**
     * Recupera el detalle de una categoría por id con su conteo de documentos ACTIVE.
     *
     * @throws ResourceNotFoundException {@code 404} si el id no existe.
     */
    @Transactional(readOnly = true)
    public CategoryDetailResponse findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("category.not-found", id)));
        long count = documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE).stream()
                .filter(p -> id.equals(p.getCategoryId()))
                .map(CategoryDocumentCount::getCount)
                .findFirst()
                .orElse(0L);
        return CategoryMapper.toDetailResponse(category, count);
    }

    /**
     * Crea una nueva categoría documental con {@code status = ACTIVE}.
     *
     * <p>Reglas:
     * <ul>
     *   <li>El nombre debe ser único en el sistema (activas e inactivas incluidas).</li>
     *   <li>Al finalizar se registra la acción {@code CREATE_CATEGORY} con detalle
     *       {@code "Categoria creada: {nombre}"} (contrato CAT-03).</li>
     * </ul>
     *
     * @throws ConflictException {@code 409} si ya existe una categoría con ese nombre.
     */
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

        return CategoryMapper.toCreateResponse(saved, messageResolver.get("category.created.success"));
    }

    /**
     * Edita el nombre y/o la descripción de una categoría existente.
     *
     * <p>Reglas:
     * <ul>
     *   <li>La categoría debe existir (404) y estar en estado {@code ACTIVE} (403).</li>
     *   <li>Si el nombre cambia, debe ser único excluyendo la propia categoría (409).</li>
     *   <li>Si {@code description} es {@code null}, se conserva la descripción actual.</li>
     *   <li>Al finalizar se registra la acción {@code EDIT_CATEGORY} con detalle
     *       {@code "Campos modificados: [field1, ...]"} (contrato CAT-04).</li>
     * </ul>
     *
     * @throws ResourceNotFoundException {@code 404} si el id no existe.
     * @throws BusinessRuleException     {@code 403} si la categoría está INACTIVE.
     * @throws ConflictException         {@code 409} si el nuevo nombre ya pertenece a otra categoría.
     */
    @Transactional
    public UpdateCategoryResponse update(Long id, UpdateCategoryRequest request, AuditContext audit) {
        requireActorUserId(audit);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("category.not-found", id)));

        if (category.getStatus() == CategoryStatus.INACTIVE) {
            throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN,
                    messageResolver.get("category.inactive.cannot-edit"));
        }

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

        return CategoryMapper.toUpdateResponse(updated, messageResolver.get("category.updated.success"));
    }

    /**
     * Activa o desactiva una categoría existente.
     *
     * <p>Reglas:
     * <ul>
     *   <li>La categoría debe existir (404).</li>
     *   <li>Si ya tiene el estado solicitado, se lanza 400.</li>
     *   <li>Al finalizar se registra la acción {@code DEACTIVATE_CATEGORY} con detalle
     *       {@code "Estado cambiado a {ACTIVE|INACTIVE}"} (contrato CAT-05).</li>
     * </ul>
     *
     * @throws ResourceNotFoundException {@code 404} si el id no existe.
     * @throws BusinessRuleException     {@code 400} si la categoría ya tiene el estado solicitado.
     */
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

        return CategoryMapper.toStatusResponse(updated, message);
    }

    private List<String> applyUpdates(Category category, UpdateCategoryRequest request, boolean nameChanged) {
        List<String> modifiedFields = new ArrayList<>();

        if (nameChanged) {
            category.setName(request.name());
            modifiedFields.add("name");
        }

        if (request.description() != null && !request.description().equals(category.getDescription())) {
            category.setDescription(request.description());
            modifiedFields.add("description");
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
