package co.edu.docurural.document.mapper;

import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;

import java.util.Objects;

/**
 * Mapper estático entre la entidad {@link Document} y los DTOs públicos del módulo.
 */
public final class DocumentMapper {

    private DocumentMapper() {
    }

    /**
     * Construye la respuesta de carga de documento (DOC-03 / HU-09).
     */
    public static UploadDocumentResponse toUploadResponse(Document document, String message) {
        Objects.requireNonNull(document, "document no puede ser null");
        return new UploadDocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getCategory().getName(),
                document.getResponsibleArea(),
                document.getDocumentDate(),
                document.getFileFormat() != null ? document.getFileFormat().name() : null,
                document.getFileSizeBytes(),
                document.getOriginalFileName(),
                document.getCreatedAt(),
                message
        );
    }
}
