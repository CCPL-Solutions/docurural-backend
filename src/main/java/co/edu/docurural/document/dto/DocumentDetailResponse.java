package co.edu.docurural.document.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentDetailResponse(
        Long id,
        String title,
        String description,
        CategoryRef category,
        String responsibleArea,
        LocalDate documentDate,
        String fileFormat,
        Long fileSizeBytes,
        String originalFileName,
        UploadedByRef uploadedBy,
        LocalDateTime createdAt
) {
    public record CategoryRef(Long id, String name) {}

    public record UploadedByRef(Long id, String fullName) {}
}
