package co.edu.docurural.document.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.document.dto.DocumentFileContent;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.storage.FileStorageService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * Acceso al contenido binario de documentos: visualización y descarga
 * (DOC-07 / HU-11, DOC-08 / HU-12).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentContentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;

    /**
     * Carga el archivo binario para visualización en línea (DOC-07 / HU-11).
     *
     * @throws IllegalArgumentException  si el contexto de auditoría es inválido.
     * @throws ResourceNotFoundException si el documento no existe, está DELETED o el archivo no está disponible.
     */
    @Transactional
    public DocumentFileContent openForView(Long id, AuditContext audit) {
        return loadAndAudit(id, audit, ActivityAction.VIEW,
                doc -> "Formato: " + doc.getFileFormat().name(),
                "visualizado");
    }

    /**
     * Carga el archivo binario para descarga (DOC-08 / HU-12).
     *
     * @throws IllegalArgumentException  si el contexto de auditoría es inválido.
     * @throws ResourceNotFoundException si el documento no existe, está DELETED o el archivo no está disponible.
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
        if (audit == null || audit.actorUserId() == null) {
            throw new IllegalArgumentException("audit.actorUserId no puede ser null");
        }

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
}
