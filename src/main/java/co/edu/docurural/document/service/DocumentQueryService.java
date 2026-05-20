package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.mapper.DocumentMapper;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.repository.projection.CategoryDocumentCount;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.shared.util.SortingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consultas de solo lectura sobre documentos (DOC-01, DOC-02 / HU-11, HU-15).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentQueryService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIR = "desc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "title", "documentDate");

    private final DocumentRepository documentRepository;
    private final MessageResolver messageResolver;

    /**
     * Lista paginada de documentos ACTIVE (DOC-01 / HU-15).
     */
    @Transactional(readOnly = true)
    public DocumentListResponse list(Integer page, Integer size, String sortBy, String sortDir) {
        Pageable pageable = SortingValidator.resolvePageable(
                page, size, sortBy, sortDir,
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_BY, DEFAULT_SORT_DIR,
                DEFAULT_PAGE, DEFAULT_SIZE, MAX_SIZE,
                messageResolver.get("document.page.invalid"),
                messageResolver.get("document.page.size-invalid"),
                messageResolver.get("document.sort.unsupported-field", sortBy),
                messageResolver.get("document.sort.unsupported-direction", sortDir));

        int resolvedPage = (page == null) ? DEFAULT_PAGE : page;
        int resolvedSize = (size == null) ? DEFAULT_SIZE : size;
        Page<Document> pageResult = documentRepository.findByStatus(DocumentStatus.ACTIVE, pageable);

        log.debug("Listado de documentos: page={} size={} sortBy={} sortDir={} total={}",
                resolvedPage, resolvedSize, sortBy, sortDir, pageResult.getTotalElements());

        return DocumentMapper.toListResponse(pageResult, resolvedPage, resolvedSize);
    }

    /**
     * Ficha completa de un documento activo (DOC-02 / HU-11).
     */
    @Transactional(readOnly = true)
    public DocumentDetailResponse findDetailById(Long id) {
        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.not-found", id)));
        return DocumentMapper.toDetailResponse(document);
    }

    /**
     * Conteos de documentos ACTIVE agrupados por categoría.
     * Usado por {@code CategoryService} para evitar acceso directo al repositorio de documentos.
     *
     * @return mapa de {@code categoryId → count}; categorías sin documentos no aparecen.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getActiveCountsByCategory() {
        return documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        CategoryDocumentCount::getCategoryId,
                        CategoryDocumentCount::getCount));
    }
}
