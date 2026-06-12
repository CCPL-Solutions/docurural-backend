package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.BatchUploadDocumentRequestDto;
import co.edu.docurural.document.dto.BatchUploadDocumentResponseDto;
import co.edu.docurural.shared.audit.AuditContext;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentBatchService {

    int MAX_FILES_PER_BATCH = 5;

    BatchUploadDocumentResponseDto uploadBatch(BatchUploadDocumentRequestDto request,
                                               MultipartFile[] files,
                                               AuditContext audit);
}
