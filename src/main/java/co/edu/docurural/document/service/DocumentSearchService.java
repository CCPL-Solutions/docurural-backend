package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.dto.FilterOptionsResponse;
import co.edu.docurural.shared.audit.AuditContext;

import java.time.LocalDate;

public interface DocumentSearchService {

    DocumentListResponse search(
            String q, Long categoryId, String responsibleArea,
            LocalDate dateFrom, LocalDate dateTo, Long uploadedBy,
            Integer page, Integer size, String sortBy, String sortDir,
            boolean actorIsAdmin, AuditContext audit);

    FilterOptionsResponse getFilterOptions(boolean actorIsAdmin);
}
