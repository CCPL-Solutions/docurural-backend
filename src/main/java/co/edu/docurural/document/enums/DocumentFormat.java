package co.edu.docurural.document.enums;

import org.springframework.http.MediaType;

public enum DocumentFormat {
    PDF,
    DOCX,
    XLSX,
    JPG,
    PNG;

    public MediaType toMediaType() {
        return switch (this) {
            case PDF -> MediaType.APPLICATION_PDF;
            case JPG -> MediaType.IMAGE_JPEG;
            case PNG -> MediaType.IMAGE_PNG;
            case DOCX -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case XLSX -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        };
    }
}
