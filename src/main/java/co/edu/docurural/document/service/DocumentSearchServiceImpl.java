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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSearchServiceImpl implements DocumentSearchService {

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

    @Override
    @Transactional
    public DocumentListResponse search(
            String q, Long categoryId, String responsibleArea,
            LocalDate dateFrom, LocalDate dateTo, Long uploadedBy,
            Integer page, Integer size, String sortBy, String sortDir,
            boolean actorIsAdmin, AuditContext audit) {

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

        String normalizedArea = (responsibleArea == null) ? null : responsibleArea.trim();
        if (normalizedArea != null && normalizedArea.isBlank()) {
            normalizedArea = null;
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.search.date-range.invalid"));
        }

        Long uploadedByEffective = actorIsAdmin ? uploadedBy : null;

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

        Specification<Document> spec = Specification.where(DocumentSpecifications.isActive())
                .and(DocumentSpecifications.matchesText(normalizedQ))
                .and(DocumentSpecifications.categoryIdEquals(categoryId))
                .and(DocumentSpecifications.responsibleAreaContains(normalizedArea))
                .and(DocumentSpecifications.documentDateGte(dateFrom))
                .and(DocumentSpecifications.documentDateLte(dateTo))
                .and(DocumentSpecifications.uploadedByEquals(uploadedByEffective));

        Page<Document> result = documentRepository.findAll(spec, pageable);

        log.debug("Búsqueda documentos: q='{}' categoryId={} responsibleArea='{}' dateFrom={} dateTo={} "
                        + "uploadedBy={} page={} size={} total={}",
                normalizedQ, categoryId, normalizedArea, dateFrom, dateTo,
                uploadedByEffective, resolvedPage, resolvedSize, result.getTotalElements());

        ActiveFiltersResponse activeFilters = buildActiveFilters(
                categoryId, normalizedArea, dateFrom, dateTo, uploadedByEffective);

        if (normalizedQ != null) {
            activityLogService.record(
                    ActivityAction.SEARCH, audit, null,
                    buildSearchDetail(normalizedQ, activeFilters, result.getTotalElements()));
        }

        return DocumentMapper.toListResponse(result, resolvedPage, resolvedSize, normalizedQ, activeFilters);
    }

    @Override
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
