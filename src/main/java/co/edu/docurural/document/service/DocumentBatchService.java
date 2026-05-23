package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.BatchUploadDocumentRequest;
import co.edu.docurural.document.dto.BatchUploadDocumentResponse;
import co.edu.docurural.shared.audit.AuditContext;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentBatchService {

    int MAX_FILES_PER_BATCH = 5;

    BatchUploadDocumentResponse uploadBatch(BatchUploadDocumentRequest request,
                                            MultipartFile[] files,
                                            AuditContext audit);
}
