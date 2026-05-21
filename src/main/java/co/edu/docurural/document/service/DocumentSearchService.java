package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.ActiveFiltersResponse;
import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.dto.FilterOptionsResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.mapper.DocumentMapper;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.repository.DocumentSpecifications;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.shared.util.SortingValidator;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.enums.UserStatus;
import co.edu.docurural.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Servicio de búsqueda y filtrado de documentos (SRC-01 / HU-20, HU-21, HU-22).
 *
 * <p>Orquesta la composición dinámica de predicados ({@link DocumentSpecifications}),
 * la validación de parámetros, la resolución de nombres para {@code activeFilters}
 * y la auditoría condicional de la acción {@link ActivityAction#SEARCH}.
 *
 * <h3>Reglas de auditoría</h3>
 * <ul>
 *   <li>Se registra {@code SEARCH} en {@code activity_log} SOLO cuando {@code q} está presente (HU-20, HU-22).</li>
 *   <li>Las consultas de solo filtros sin texto (HU-21) NO generan registro de auditoría.</li>
 * </ul>
 *
 * <h3>Manejo de {@code uploadedBy}</h3>
 * <p>El parámetro se aplica únicamente si {@code actorIsAdmin == true}. Para roles EDITOR y READER
 * se descarta silenciosamente sin devolver error, conforme a la spec SRC-01.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSearchService {

    private static final int MIN_Q = 2;
    private static final int MAX_Q = 100;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIR = "desc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "title", "documentDate");

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;

    /**
     * Busca y filtra documentos activos aplicando todos los criterios con operador AND (SRC-01).
     *
     * <p>Todos los parámetros de búsqueda y filtrado son opcionales. Un {@code q} blank tras trim
     * se trata como ausente. El campo {@code uploadedBy} es ignorado silenciosamente si
     * {@code actorIsAdmin} es {@code false}.
     *
     * @param q               texto libre de búsqueda (2–100 chars tras trim); {@code null} o blank omite el filtro de texto
     * @param categoryId      id de categoría; {@code null} omite el filtro
     * @param responsibleArea área responsable (ILIKE parcial); {@code null} o blank omite el filtro
     * @param dateFrom        fecha mínima de {@code document_date}; {@code null} omite el límite inferior
     * @param dateTo          fecha máxima de {@code document_date}; {@code null} omite el límite superior
     * @param uploadedBy      id del usuario que cargó; respetado solo si {@code actorIsAdmin == true}
     * @param page            número de página 1-based (default {@value DEFAULT_PAGE})
     * @param size            tamaño de página (default {@value DEFAULT_SIZE}, máx {@value MAX_SIZE})
     * @param sortBy          campo de ordenamiento ({@code createdAt}, {@code title}, {@code documentDate})
     * @param sortDir         dirección: {@code asc} o {@code desc}
     * @param actorIsAdmin    {@code true} si el usuario autenticado tiene rol {@code ADMIN}
     * @param audit           contexto de auditoría con actor y dirección IP
     * @return envelope paginado con los documentos encontrados y la metadata de búsqueda/filtros
     */
    @Transactional
    public DocumentListResponse search(
            String q, Long categoryId, String responsibleArea,
            LocalDate dateFrom, LocalDate dateTo, Long uploadedBy,
            Integer page, Integer size, String sortBy, String sortDir,
            boolean actorIsAdmin, AuditContext audit) {

        // 1. Normalizar q: trim → null si blank; validar longitud mínima y máxima
        String normalizedQ = (q == null) ? null : q.trim();
        if (normalizedQ != null && normalizedQ.isBlank()) {
            normalizedQ = null;
        }
        if (normalizedQ != null && normalizedQ.length() < MIN_Q) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.search.q.too-short"));
        }
        if (normalizedQ != null && normalizedQ.length() > MAX_Q) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.search.q.too-long"));
        }

        // 2. Normalizar responsibleArea: trim → null si blank
        String normalizedArea = (responsibleArea == null) ? null : responsibleArea.trim();
        if (normalizedArea != null && normalizedArea.isBlank()) {
            normalizedArea = null;
        }

        // 3. Validar rango de fechas
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.search.date-range.invalid"));
        }

        // 4. uploadedBy: ignorar silenciosamente si el actor no es ADMIN
        Long uploadedByEffective = actorIsAdmin ? uploadedBy : null;

        // 5. Resolver paginación y ordenamiento (delega validación al helper compartido)
        Pageable pageable = SortingValidator.resolvePageable(
                page, size, sortBy, sortDir,
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_BY, DEFAULT_SORT_DIR,
                DEFAULT_PAGE, DEFAULT_SIZE, MAX_SIZE,
                messageResolver.get("document.page.invalid"),
                messageResolver.get("document.page.size-invalid"),
                messageResolver.get("document.sort.unsupported-field", sortBy),
                messageResolver.get("document.sort.unsupported-direction", sortDir));

        int resolvedPage = pageable.getPageNumber() + 1;
        int resolvedSize = pageable.getPageSize();

        // 6. Componer Specification dinámica (AND de todos los predicados no nulos)
        Specification<Document> spec = Specification.where(DocumentSpecifications.isActive())
                .and(DocumentSpecifications.matchesText(normalizedQ))
                .and(DocumentSpecifications.categoryIdEquals(categoryId))
                .and(DocumentSpecifications.responsibleAreaContains(normalizedArea))
                .and(DocumentSpecifications.documentDateGte(dateFrom))
                .and(DocumentSpecifications.documentDateLte(dateTo))
                .and(DocumentSpecifications.uploadedByEquals(uploadedByEffective));

        // 7. Ejecutar query paginada
        Page<Document> result = documentRepository.findAll(spec, pageable);

        log.debug("Búsqueda documentos: q='{}' categoryId={} responsibleArea='{}' dateFrom={} dateTo={} "
                        + "uploadedBy={} page={} size={} total={}",
                normalizedQ, categoryId, normalizedArea, dateFrom, dateTo,
                uploadedByEffective, resolvedPage, resolvedSize, result.getTotalElements());

        // 8. Construir activeFilters (null si no hubo ningún filtro)
        ActiveFiltersResponse activeFilters = buildActiveFilters(
                categoryId, normalizedArea, dateFrom, dateTo, uploadedByEffective);

        // 9. Auditar SOLO cuando q está presente (HU-20, HU-22); solo filtros no se auditan (HU-21)
        if (normalizedQ != null) {
            activityLogService.record(
                    ActivityAction.SEARCH, audit, null,
                    buildSearchDetail(normalizedQ, activeFilters, result.getTotalElements()));
        }

        return DocumentMapper.toListResponse(result, resolvedPage, resolvedSize, normalizedQ, activeFilters);
    }

    /**
     * Retorna las opciones disponibles para los selectores del panel de filtros (SRC-02 / HU-21).
     *
     * <p>La lista de usuarios solo se incluye cuando {@code actorIsAdmin == true}; en caso contrario
     * el campo {@code users} del response es {@code null}.
     *
     * @param actorIsAdmin {@code true} si el actor tiene rol {@code ADMIN}
     * @return categorías activas y, solo para ADMIN, usuarios activos ordenados por nombre
     */
    @Transactional(readOnly = true)
    public FilterOptionsResponse getFilterOptions(boolean actorIsAdmin) {
        List<FilterOptionsResponse.CategoryOption> categories = categoryRepository.findAll().stream()
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .sorted(Comparator.comparing(Category::getName))
                .map(c -> new FilterOptionsResponse.CategoryOption(c.getId(), c.getName()))
                .toList();

        List<FilterOptionsResponse.UserOption> users = null;
        if (actorIsAdmin) {
            users = userRepository.findAll().stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .sorted(Comparator.comparing(User::getFullName))
                    .map(u -> new FilterOptionsResponse.UserOption(u.getId(), u.getFullName()))
                    .toList();
        }

        return new FilterOptionsResponse(categories, users);
    }

    private ActiveFiltersResponse buildActiveFilters(
            Long categoryId, String responsibleArea,
            LocalDate dateFrom, LocalDate dateTo, Long uploadedByEffective) {

        boolean hasFilters = categoryId != null || responsibleArea != null
                || dateFrom != null || dateTo != null || uploadedByEffective != null;

        if (!hasFilters) return null;

        String categoryName = null;
        if (categoryId != null) {
            categoryName = categoryRepository.findById(categoryId)
                    .map(Category::getName)
                    .orElse(null);
        }

        String uploadedByName = null;
        if (uploadedByEffective != null) {
            uploadedByName = userRepository.findById(uploadedByEffective)
                    .map(User::getFullName)
                    .orElse(null);
        }

        return new ActiveFiltersResponse(categoryId, categoryName, responsibleArea, dateFrom, dateTo, uploadedByName);
    }

    private String buildSearchDetail(String q, ActiveFiltersResponse activeFilters, long totalResults) {
        String filtersStr = (activeFilters == null) ? "sin filtros" : formatFilters(activeFilters);
        return String.format("Término: \"%s\"; Filtros: %s; Resultados: %d", q, filtersStr, totalResults);
    }

    private String formatFilters(ActiveFiltersResponse f) {
        StringBuilder sb = new StringBuilder();
        if (f.categoryName() != null) append(sb, "categoría=" + f.categoryName());
        if (f.responsibleArea() != null) append(sb, "área=" + f.responsibleArea());
        if (f.dateFrom() != null) append(sb, "desde=" + f.dateFrom());
        if (f.dateTo() != null) append(sb, "hasta=" + f.dateTo());
        if (f.uploadedByName() != null) append(sb, "subidoPor=" + f.uploadedByName());
        return sb.isEmpty() ? "sin filtros" : sb.toString();
    }

    private void append(StringBuilder sb, String part) {
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(part);
    }
}
