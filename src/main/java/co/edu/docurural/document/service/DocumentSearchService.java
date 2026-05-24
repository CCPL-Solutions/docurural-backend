package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DocumentListResponseDto;
import co.edu.docurural.document.dto.FilterOptionsResponseDto;
import co.edu.docurural.shared.audit.AuditContext;

import java.time.LocalDate;

public interface DocumentSearchService {

    DocumentListResponseDto search(
            String q, Long categoryId, String responsibleArea,
            LocalDate dateFrom, LocalDate dateTo, Long uploadedBy,
            Integer page, Integer size, String sortBy, String sortDir,
            boolean actorIsAdmin, AuditContext audit);

    FilterOptionsResponseDto getFilterOptions(boolean actorIsAdmin);
}
