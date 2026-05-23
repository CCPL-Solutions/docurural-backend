package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DeleteDocumentResponse;
import co.edu.docurural.document.dto.UpdateDocumentMetadataRequest;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponse;
import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.shared.audit.AuditContext;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface DocumentCommandService {

    UploadDocumentResponse upload(UploadDocumentRequest request, MultipartFile file, AuditContext audit);

    UpdateDocumentMetadataResponse updateMetadata(Long id, UpdateDocumentMetadataRequest request, AuditContext audit);

    DeleteDocumentResponse deleteLogical(Long id, AuditContext audit);

    /**
     * Carga un archivo individual dentro de un lote (DOC-04 / HU-10).
     * Invocado desde {@link DocumentBatchService} para garantizar transacción por archivo.
     */
    Document uploadSingleForBatch(MultipartFile file, String title, Long categoryId,
                                   String responsibleArea, LocalDate documentDate, AuditContext audit);
}
