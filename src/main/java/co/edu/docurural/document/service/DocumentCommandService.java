package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DeleteDocumentResponseDto;
import co.edu.docurural.document.dto.UpdateDocumentMetadataRequestDto;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponseDto;
import co.edu.docurural.document.dto.UploadDocumentRequestDto;
import co.edu.docurural.document.dto.UploadDocumentResponseDto;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.shared.audit.AuditContext;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface DocumentCommandService {

    UploadDocumentResponseDto upload(UploadDocumentRequestDto request, MultipartFile file, AuditContext audit);

    UpdateDocumentMetadataResponseDto updateMetadata(Long id, UpdateDocumentMetadataRequestDto request, AuditContext audit);

    DeleteDocumentResponseDto deleteLogical(Long id, AuditContext audit);

    /**
     * Carga un archivo individual dentro de un lote (DOC-04 / HU-10).
     * Invocado desde {@link DocumentBatchService} para garantizar transacción por archivo.
     */
    Document uploadSingleForBatch(MultipartFile file, String title, Long categoryId,
                                   String responsibleArea, LocalDate documentDate, AuditContext audit);
}
