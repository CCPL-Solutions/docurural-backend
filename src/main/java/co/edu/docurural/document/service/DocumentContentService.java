package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DocumentFileContent;
import co.edu.docurural.shared.audit.AuditContext;

public interface DocumentContentService {

    DocumentFileContent openForView(Long id, AuditContext audit);

    DocumentFileContent openForDownload(Long id, AuditContext audit);
}
