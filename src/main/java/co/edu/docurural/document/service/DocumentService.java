package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.DeleteDocumentResponse;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentFileContent;
import co.edu.docurural.document.dto.UpdateDocumentMetadataRequest;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponse;
import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.mapper.DocumentMapper;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.storage.FileStorageService;
import co.edu.docurural.document.storage.StoredFile;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.domain.entity.User;
import co.edu.docurural.shared.domain.enums.UserRole;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Servicio del módulo de documentos (DOC-02..DOC-08 / HU-09..HU-14).
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
     * @throws BusinessRuleException     {@code INVALID_ARGUMENT (400)} si el archivo está vacío.
     * @throws ResourceNotFoundException {@code 404} si la categoría no existe o está INACTIVE.
     * @throws BusinessRuleException     {@code PAYLOAD_TOO_LARGE (413)} si supera 10 MB.
     * @throws BusinessRuleException     {@code UNSUPPORTED_MEDIA_TYPE (415)} si el MIME no está permitido.
     */
    @Transactional
    public UploadDocumentResponse upload(UploadDocumentRequest request, MultipartFile file, AuditContext audit) {
        Long actorId = requireActorUserId(audit);

        Category category = categoryRepository.findById(request.categoryId())
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.category.not-found")));

        Document saved = processSingleFile(
                file,
                request.title(),
                category,
                request.responsibleArea(),
                request.documentDate(),
                request.description(),
                "Archivo: ",
                actorId,
                audit);

        log.info("Documento cargado: id={} title='{}' format={} uploadedBy={}",
                saved.getId(), saved.getTitle(), saved.getFileFormat(), actorId);

        return DocumentMapper.toUploadResponse(saved, messageResolver.get("document.uploaded.success"));
    }

    /**
     * Edita metadatos de un documento activo (DOC-05 / HU-13).
     *
     * <p>Reglas de permiso:
     * <ul>
     *   <li>{@code ADMIN}: puede editar cualquier documento.</li>
     *   <li>{@code EDITOR}: solo documentos cargados por el mismo usuario.</li>
     * </ul>
     *
     * <p>Reglas de actualización:
     * <ul>
     *   <li>Si {@code description} llega en {@code null}, se conserva la actual.</li>
     *   <li>Si no hay cambios efectivos, retorna 200 y no registra actividad.</li>
     * </ul>
     */
    @Transactional
    public UpdateDocumentMetadataResponse updateMetadata(Long id, UpdateDocumentMetadataRequest request, AuditContext audit) {
        Long actorId = requireActorUserId(audit);

        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.not-found", id)));

        Category category = categoryRepository.findById(request.categoryId())
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.category.not-found")));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("user.not-found", actorId)));

        boolean canEditAny = actor.getRole() == UserRole.ADMIN;
        boolean canEditOwn = actor.getRole() == UserRole.EDITOR
                && actorId.equals(document.getUploadedBy().getId());
        if (!canEditAny && !canEditOwn) {
            throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN,
                    messageResolver.get("document.edit.forbidden"));
        }

        List<String> modifiedFields = applyMetadataUpdates(document, request, category);
        if (modifiedFields.isEmpty()) {
            log.info("Edición de metadatos sin cambios: documentId={} requestedBy={}", id, actorId);
            return DocumentMapper.toUpdateMetadataResponse(
                    document, messageResolver.get("document.updated.no-changes"));
        }

        Document updated = documentRepository.save(document);
        activityLogService.record(
                ActivityAction.EDIT_DOC,
                audit,
                updated.getId(),
                "Campos modificados: " + modifiedFields);

        log.info("Metadatos actualizados: documentId={} modifiedFields={} requestedBy={}",
                updated.getId(), modifiedFields, actorId);

        return DocumentMapper.toUpdateMetadataResponse(
                updated, messageResolver.get("document.updated.success"));
    }

    /**
     * Elimina lógicamente un documento activo (DOC-06 / HU-14).
     *
     * <p>No elimina el archivo físico del almacenamiento local del MVP.
     * Si el documento no existe o ya está {@code DELETED}, retorna 404 (no idempotente).
     */
    @Transactional
    public DeleteDocumentResponse deleteLogical(Long id, AuditContext audit) {
        Long actorId = requireActorUserId(audit);

        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.not-found", id)));

        document.setStatus(DocumentStatus.DELETED);
        Document deleted = documentRepository.save(document);

        activityLogService.record(
                ActivityAction.DELETE_DOC,
                audit,
                deleted.getId(),
                "Título: " + deleted.getTitle());

        log.info("Documento eliminado lógicamente: documentId={} requestedBy={}", deleted.getId(), actorId);

        return DocumentMapper.toDeleteResponse(deleted, messageResolver.get("document.deleted.success"));
    }

    /**
     * Carga un archivo individual dentro de un lote (DOC-04 / HU-10).
     *
     * <p>Invocado desde {@link DocumentBatchService} — al cruzar el límite del bean, el proxy de
     * Spring aplica la transacción aislada por archivo, lo que permite el comportamiento best-effort.
     *
     * @param categoryId ya fue validado como ACTIVE por el orquestador; se vuelve a leer aquí
     *                   para tenerlo en el contexto transaccional propio.
     * @return la entidad {@link Document} persistida.
     * @throws BusinessRuleException     si el archivo está vacío, supera el tamaño o el MIME no está permitido.
     * @throws ResourceNotFoundException si la categoría dejó de estar ACTIVE entre la validación del orquestador y esta TX.
     */
    @Transactional
    public Document uploadSingleForBatch(MultipartFile file,
                                         String title,
                                         Long categoryId,
                                         String responsibleArea,
                                         LocalDate documentDate,
                                         AuditContext audit) {
        Long actorId = requireActorUserId(audit);

        Category category = categoryRepository.findById(categoryId)
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.category.not-found")));

        Document saved = processSingleFile(
                file,
                title,
                category,
                responsibleArea,
                documentDate,
                null,
                "Carga múltiple — Archivo: ",
                actorId,
                audit);

        log.info("Documento cargado (lote): id={} title='{}' format={} uploadedBy={}",
                saved.getId(), saved.getTitle(), saved.getFileFormat(), actorId);

        return saved;
    }

    private List<String> applyMetadataUpdates(Document document,
                                              UpdateDocumentMetadataRequest request,
                                              Category category) {
        List<String> modifiedFields = new ArrayList<>();

        if (!request.title().equals(document.getTitle())) {
            document.setTitle(request.title());
            modifiedFields.add("title");
        }

        if (!category.getId().equals(document.getCategory().getId())) {
            document.setCategory(category);
            modifiedFields.add("categoryId");
        }

        if (!request.responsibleArea().equals(document.getResponsibleArea())) {
            document.setResponsibleArea(request.responsibleArea());
            modifiedFields.add("responsibleArea");
        }

        if (!request.documentDate().equals(document.getDocumentDate())) {
            document.setDocumentDate(request.documentDate());
            modifiedFields.add("documentDate");
        }

        if (request.description() != null && !request.description().equals(document.getDescription())) {
            document.setDescription(request.description());
            modifiedFields.add("description");
        }

        return modifiedFields;
    }

    private Document processSingleFile(MultipartFile file,
                                       String title,
                                       Category category,
                                       String responsibleArea,
                                       LocalDate documentDate,
                                       String description,
                                       String activityDetailPrefix,
                                       Long actorId,
                                       AuditContext audit) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.file.empty"));
        }

        DocumentFormat format = fileValidationService.validate(file);

        StoredFile stored = fileStorageService.store(file, format);

        // Limpieza compensatoria: si la transacción hace rollback, el archivo en
        // disco quedaría huérfano. La comprobación previa evita el
        // IllegalStateException en tests sin contexto transaccional activo.
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
                .title(title)
                .description(description)
                .category(category)
                .responsibleArea(responsibleArea)
                .documentDate(documentDate)
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
                activityDetailPrefix + saved.getOriginalFileName());

        return saved;
    }

    /**
     * Retorna la ficha completa de metadatos de un documento activo (DOC-02 / HU-11).
     *
     * @throws ResourceNotFoundException {@code 404} si el documento no existe o tiene estado DELETED.
     */
    @Transactional(readOnly = true)
    public DocumentDetailResponse findDetailById(Long id) {
        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.not-found", id)));
        return DocumentMapper.toDetailResponse(document);
    }

    /**
     * Carga el archivo binario de un documento para visualización en línea (DOC-07 / HU-11).
     *
     * <p>El registro de actividad {@code VIEW} se genera sólo si el archivo existe en disco.
     * Si el archivo físico no se encuentra, se lanza {@link ResourceNotFoundException} antes de registrar.
     *
     * @throws IllegalArgumentException  si {@code audit} o {@code audit.actorUserId()} es null.
     * @throws ResourceNotFoundException {@code 404} si el documento no existe, está DELETED o el archivo físico no está disponible.
     */
    @Transactional
    public DocumentFileContent openForView(Long id, AuditContext audit) {
        return loadAndAudit(id, audit, ActivityAction.VIEW,
                doc -> "Formato: " + doc.getFileFormat().name(),
                "visualizado");
    }

    /**
     * Carga el archivo binario de un documento para descarga (DOC-08 / HU-12).
     *
     * <p>El registro de actividad {@code DOWNLOAD} se genera sólo si el archivo existe en disco.
     * Si el archivo físico no se encuentra, se lanza {@link ResourceNotFoundException} antes de registrar.
     *
     * @throws IllegalArgumentException  si {@code audit} o {@code audit.actorUserId()} es null.
     * @throws ResourceNotFoundException {@code 404} si el documento no existe, está DELETED o el archivo físico no está disponible.
     */
    @Transactional
    public DocumentFileContent openForDownload(Long id, AuditContext audit) {
        return loadAndAudit(id, audit, ActivityAction.DOWNLOAD,
                doc -> "Archivo: " + doc.getOriginalFileName(),
                "descargado");
    }

    private DocumentFileContent loadAndAudit(Long id, AuditContext audit,
                                             ActivityAction action,
                                             Function<Document, String> detailBuilder,
                                             String logVerb) {
        requireActorUserId(audit);

        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.not-found", id)));

        Resource resource = fileStorageService.load(document.getFilePath());

        activityLogService.record(action, audit, document.getId(), detailBuilder.apply(document));

        log.debug("Documento {}: id={} format={} actor={}",
                logVerb, document.getId(), document.getFileFormat(), audit.actorUserId());

        return new DocumentFileContent(resource, document.getFileFormat(),
                document.getOriginalFileName(), document.getFileSizeBytes());
    }

    Long requireActorUserId(AuditContext audit) {
        if (audit == null) {
            throw new IllegalArgumentException("audit no puede ser null");
        }
        if (audit.actorUserId() == null) {
            throw new IllegalArgumentException("audit.actorUserId no puede ser null");
        }
        return audit.actorUserId();
    }
}
