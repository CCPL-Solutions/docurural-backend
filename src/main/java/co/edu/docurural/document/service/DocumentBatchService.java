package co.edu.docurural.document.service;

import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.BatchUploadDocumentRequest;
import co.edu.docurural.document.dto.BatchUploadDocumentResponse;
import co.edu.docurural.document.dto.BatchUploadItemResult;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestador de la carga en lote de documentos (DOC-04 / HU-10).
 *
 * <p>Cada archivo se procesa en su propia transacción delegando a
 * {@link DocumentService#uploadSingleForBatch}, lo que permite el comportamiento
 * best-effort: si un archivo falla, los demás se persisten normalmente.
 *
 * <p>Este bean existe separado de {@link DocumentService} para que el proxy transaccional
 * de Spring se aplique al cruzar el límite de bean (la auto-invocación no activa el proxy).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentBatchService {

    static final int MAX_FILES_PER_BATCH = 5;

    private final DocumentCommandService documentService;
    private final CategoryRepository categoryRepository;
    private final MessageResolver messageResolver;

    /**
     * Procesa un lote de hasta {@value MAX_FILES_PER_BATCH} archivos.
     *
     * <p>Valida los campos comunes y la categoría antes de iterar. Los errores individuales
     * por archivo (tamaño, MIME, almacenamiento) no detienen el lote.
     *
     * @throws IllegalArgumentException  si el contexto de auditoría es inválido.
     * @throws BusinessRuleException     {@code 400} si el lote está vacío, supera el máximo o
     *                                   la lista de títulos excede la de archivos.
     * @throws ResourceNotFoundException {@code 404} si la categoría no existe o está INACTIVE.
     */
    public BatchUploadDocumentResponse uploadBatch(BatchUploadDocumentRequest request,
                                                   MultipartFile[] files,
                                                   AuditContext audit) {
        documentService.requireActorUserId(audit);

        if (files == null || files.length == 0) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.batch.empty"));
        }
        if (files.length > MAX_FILES_PER_BATCH) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.batch.too-many-files", MAX_FILES_PER_BATCH));
        }

        List<String> titles = request.titles();
        if (titles != null && titles.size() > files.length) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.batch.titles-mismatch"));
        }

        // Valida la categoría una sola vez — si falla, aborta todo el lote (HTTP 404).
        categoryRepository.findById(request.categoryId())
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.category.not-found")));

        List<BatchUploadItemResult> results = new ArrayList<>(files.length);

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo_" + (i + 1);
            String resolvedTitle = resolveTitle(titles, i, fileName);

            results.add(processOneFile(file, fileName, resolvedTitle, request, audit));
        }

        int successful = (int) results.stream().filter(BatchUploadItemResult::success).count();
        int total = results.size();

        log.info("Lote completado: {}/{} archivos exitosos", successful, total);

        return new BatchUploadDocumentResponse(total, successful, total - successful, results);
    }

    private BatchUploadItemResult processOneFile(MultipartFile file,
                                                 String fileName,
                                                 String resolvedTitle,
                                                 BatchUploadDocumentRequest request,
                                                 AuditContext audit) {
        try {
            Document saved = documentService.uploadSingleForBatch(
                    file,
                    resolvedTitle,
                    request.categoryId(),
                    request.responsibleArea(),
                    LocalDate.now(),
                    audit);
            return new BatchUploadItemResult(fileName, true, saved.getId(), null);
        } catch (BusinessRuleException e) {
            log.warn("Archivo '{}' rechazado en lote: {}", fileName, e.getMessage());
            return new BatchUploadItemResult(fileName, false, null, e.getMessage());
        } catch (FileStorageException e) {
            log.error("Error de almacenamiento para '{}' en lote: {}", fileName, e.getMessage());
            return new BatchUploadItemResult(fileName, false, null, e.getMessage());
        }
    }

    private String resolveTitle(List<String> titles, int index, String fallback) {
        if (titles != null && index < titles.size()) {
            String title = titles.get(index);
            if (title != null && !title.isBlank()) {
                return title;
            }
        }
        return fallback;
    }
}
