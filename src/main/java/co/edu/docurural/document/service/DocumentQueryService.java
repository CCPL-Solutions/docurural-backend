package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.mapper.DocumentMapper;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.repository.projection.CategoryDocumentCount;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consultas de solo lectura sobre documentos (DOC-02 / HU-11).
 *
 * <p>El listado paginado con búsqueda y filtrado (DOC-01 / HU-15, HU-20, HU-21, HU-22)
 * se gestiona en {@link DocumentSearchService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentQueryService {

    private final DocumentRepository documentRepository;
    private final MessageResolver messageResolver;

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
