package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.mapper.DocumentMapper;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.storage.FileStorageService;
import co.edu.docurural.document.storage.StoredFile;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Servicio del módulo de documentos (DOC-03 / HU-09).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final FileValidationService fileValidationService;
    private final FileStorageService fileStorageService;
    private final MessageResolver messageResolver;

    /**
     * Carga un documento individual al repositorio (DOC-03 / HU-09).
     *
     * <p>Pasos:
     * <ol>
     *   <li>Valida que el actor esté autenticado.</li>
     *   <li>Verifica que el archivo no sea nulo ni vacío.</li>
     *   <li>Valida que la categoría exista y esté {@code ACTIVE}.</li>
     *   <li>Delega la validación de tamaño y MIME a {@link FileValidationService}.</li>
     *   <li>Persiste el archivo en disco vía {@link FileStorageService}.</li>
     *   <li>Guarda los metadatos en la tabla {@code documents}.</li>
     *   <li>Registra la acción {@code UPLOAD} en {@code activity_log}.</li>
     * </ol>
     *
     * @throws BusinessRuleException     {@code INVALID_ARGUMENT (400)} si el archivo está vacío.
     * @throws ResourceNotFoundException {@code 404} si la categoría no existe o está INACTIVE.
     * @throws BusinessRuleException     {@code PAYLOAD_TOO_LARGE (413)} si supera 10 MB.
     * @throws BusinessRuleException     {@code UNSUPPORTED_MEDIA_TYPE (415)} si el MIME no está permitido.
     */
    @Transactional
    public UploadDocumentResponse upload(UploadDocumentRequest request, MultipartFile file, AuditContext audit) {
        Long actorId = requireActorUserId(audit);

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.file.empty"));
        }

        Category category = categoryRepository.findById(request.categoryId())
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.category.not-found")));

        DocumentFormat format = fileValidationService.validate(file);

        StoredFile stored = fileStorageService.store(file, format);

        // Limpieza compensatoria: si la transacción hace rollback, el archivo en
        // disco quedaría huérfano. Se registra aquí para eliminarlo ante ese caso.
        // La comprobación previa evita el IllegalStateException en tests sin contexto
        // transaccional activo.
        String storedRelativePath = stored.relativePath();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        fileStorageService.delete(storedRelativePath);
                    }
                }
            });
        }

        Document document = Document.builder()
                .title(request.title())
                .description(request.description())
                .category(category)
                .responsibleArea(request.responsibleArea())
                .documentDate(request.documentDate())
                .filePath(storedRelativePath)
                .originalFileName(file.getOriginalFilename())
                .fileFormat(format)
                .fileSizeBytes(file.getSize())
                .uploadedBy(userRepository.getReferenceById(actorId))
                .build();

        Document saved = documentRepository.save(document);

        activityLogService.record(
                ActivityAction.UPLOAD,
                audit,
                saved.getId(),
                "Archivo: " + saved.getOriginalFileName());

        log.info("Documento cargado: id={} title='{}' format={} uploadedBy={}",
                saved.getId(), saved.getTitle(), saved.getFileFormat(), actorId);

        return DocumentMapper.toUploadResponse(saved, messageResolver.get("document.uploaded.success"));
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
