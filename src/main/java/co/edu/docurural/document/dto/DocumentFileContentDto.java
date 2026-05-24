package co.edu.docurural.document.dto;

import co.edu.docurural.document.enums.DocumentFormat;
import org.springframework.core.io.Resource;

public record DocumentFileContentDto(
        Resource resource,
        DocumentFormat format,
        String originalFileName,
        long fileSizeBytes
) {
}
