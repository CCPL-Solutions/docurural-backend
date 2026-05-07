package co.edu.docurural.category.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.dto.CreateCategoryRequest;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.mapper.CategoryMapper;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de gestión de categorías documentales (CAT-03 / HU-16).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;

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
