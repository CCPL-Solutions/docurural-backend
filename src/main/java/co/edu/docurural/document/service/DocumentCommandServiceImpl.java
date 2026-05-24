package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.category.entity.Category;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.document.dto.DeleteDocumentResponseDto;
import co.edu.docurural.document.dto.UpdateDocumentMetadataRequestDto;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponseDto;
import co.edu.docurural.document.dto.UploadDocumentRequestDto;
import co.edu.docurural.document.dto.UploadDocumentResponseDto;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.mapper.DocumentMapper;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.storage.FileStorageService;
import co.edu.docurural.document.storage.StoredFile;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.FieldUpdater;
import co.edu.docurural.shared.util.FileNameSanitizer;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.enums.UserRole;
import co.edu.docurural.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de escritura sobre documentos (DOC-03..DOC-06 / HU-09..HU-14).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentCommandServiceImpl implements DocumentCommandService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final FileValidationService fileValidationService;
    private final FileStorageService fileStorageService;
    private final MessageResolver messageResolver;
    private final DocumentMapper documentMapper;

    @Override
    @Transactional
    public UploadDocumentResponseDto upload(UploadDocumentRequestDto request, MultipartFile file, AuditContext audit) {
        Long actorId = requireActorUserId(audit);

        Category category = categoryRepository.findById(request.categoryId())
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.category.not-found")));

        Document saved = processSingleFile(
                file, request.title(), category,
                request.responsibleArea(), request.documentDate(), request.description(),
                "Archivo: ", actorId, audit);

        log.info("Documento cargado: id={} title='{}' format={} uploadedBy={}",
                saved.getId(), saved.getTitle(), saved.getFileFormat(), actorId);

        return documentMapper.toUploadResponse(saved, messageResolver.get("document.uploaded.success"));
    }

    @Override
    @Transactional
    public UpdateDocumentMetadataResponseDto updateMetadata(Long id, UpdateDocumentMetadataRequestDto request, AuditContext audit) {
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
            return documentMapper.toUpdateMetadataResponse(
                    document, messageResolver.get("document.updated.no-changes"));
        }

        Document updated = documentRepository.save(document);
        activityLogService.record(ActivityAction.EDIT_DOC, audit, updated.getId(),
                "Campos modificados: " + modifiedFields);

        log.info("Metadatos actualizados: documentId={} modifiedFields={} requestedBy={}",
                updated.getId(), modifiedFields, actorId);

        return documentMapper.toUpdateMetadataResponse(updated, messageResolver.get("document.updated.success"));
    }

    @Override
    @Transactional
    public DeleteDocumentResponseDto deleteLogical(Long id, AuditContext audit) {
        Long actorId = requireActorUserId(audit);

        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.not-found", id)));

        document.markAsDeleted();
        Document deleted = documentRepository.save(document);

        activityLogService.record(ActivityAction.DELETE_DOC, audit, deleted.getId(),
                "Título: " + deleted.getTitle());

        log.info("Documento eliminado lógicamente: documentId={} requestedBy={}", deleted.getId(), actorId);

        return documentMapper.toDeleteResponse(deleted, messageResolver.get("document.deleted.success"));
    }

    @Override
    @Transactional
    public Document uploadSingleForBatch(MultipartFile file, String title, Long categoryId,
                                          String responsibleArea, LocalDate documentDate, AuditContext audit) {
        Long actorId = requireActorUserId(audit);

        Category category = categoryRepository.findById(categoryId)
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("document.category.not-found")));

        Document saved = processSingleFile(
                file, title, category, responsibleArea, documentDate,
                null, "Carga múltiple — Archivo: ", actorId, audit);

        log.info("Documento cargado (lote): id={} title='{}' format={} uploadedBy={}",
                saved.getId(), saved.getTitle(), saved.getFileFormat(), actorId);

        return saved;
    }

    private List<String> applyMetadataUpdates(Document document,
                                              UpdateDocumentMetadataRequestDto request,
                                              Category category) {
        List<String> modifiedFields = new ArrayList<>(
                FieldUpdater.of(document)
                        .setIfChanged("title", request.title(), document::getTitle, document::setTitle)
                        .setIfChanged("responsibleArea", request.responsibleArea(),
                                document::getResponsibleArea, document::setResponsibleArea)
                        .setIfChanged("documentDate", request.documentDate(),
                                document::getDocumentDate, document::setDocumentDate)
                        .setIfPresent("description", request.description(),
                                document::getDescription, document::setDescription)
                        .changedFields());

        if (!category.getId().equals(document.getCategory().getId())) {
            document.setCategory(category);
            modifiedFields.add("categoryId");
        }
        return modifiedFields;
    }

    private Document processSingleFile(MultipartFile file, String title, Category category,
                                       String responsibleArea, LocalDate documentDate,
                                       String description, String activityDetailPrefix,
                                       Long actorId, AuditContext audit) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.file.empty"));
        }

        DocumentFormat format = fileValidationService.validate(file);
        StoredFile stored = fileStorageService.store(file, format);

        String storedRelativePath = stored.relativePath();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK || status == STATUS_UNKNOWN) {
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
                .originalFileName(FileNameSanitizer.sanitize(file.getOriginalFilename()))
                .fileFormat(format)
                .fileSizeBytes(file.getSize())
                .uploadedBy(userRepository.getReferenceById(actorId))
                .build();

        Document saved = documentRepository.save(document);
        activityLogService.record(ActivityAction.UPLOAD, audit, saved.getId(),
                activityDetailPrefix + saved.getOriginalFileName());

        return saved;
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
