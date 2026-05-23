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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentQueryServiceImpl implements DocumentQueryService {

    private final DocumentRepository documentRepository;
    private final MessageResolver messageResolver;

    @Override
    @Transactional(readOnly = true)
    public DocumentDetailResponse findDetailById(Long id) {
        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.not-found", id)));
        return DocumentMapper.toDetailResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getActiveCountsByCategory() {
        return documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        CategoryDocumentCount::getCategoryId,
                        CategoryDocumentCount::getCount));
    }
}
