package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DocumentFileContentDto;
import co.edu.docurural.shared.audit.AuditContext;

public interface DocumentContentService {

    DocumentFileContentDto openForView(Long id, AuditContext audit);

    DocumentFileContentDto openForDownload(Long id, AuditContext audit);
}
