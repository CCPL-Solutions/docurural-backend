package co.edu.docurural.document.mapper;

import co.edu.docurural.document.dto.DeleteDocumentResponse;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponse;
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

    /**
     * Construye la ficha completa de metadatos del documento (DOC-02 / HU-11).
     */
    public static DocumentDetailResponse toDetailResponse(Document document) {
        Objects.requireNonNull(document, "document no puede ser null");
        return new DocumentDetailResponse(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                new DocumentDetailResponse.CategoryRef(
                        document.getCategory().getId(),
                        document.getCategory().getName()),
                document.getResponsibleArea(),
                document.getDocumentDate(),
                document.getFileFormat() != null ? document.getFileFormat().name() : null,
                document.getFileSizeBytes(),
                document.getOriginalFileName(),
                new DocumentDetailResponse.UploadedByRef(
                        document.getUploadedBy().getId(),
                        document.getUploadedBy().getFullName()),
                document.getCreatedAt()
        );
    }

    /**
     * Construye la respuesta de edición de metadatos (DOC-05 / HU-13).
     */
    public static UpdateDocumentMetadataResponse toUpdateMetadataResponse(Document document, String message) {
        Objects.requireNonNull(document, "document no puede ser null");
        return new UpdateDocumentMetadataResponse(
                document.getId(),
                document.getTitle(),
                document.getCategory().getName(),
                document.getResponsibleArea(),
                document.getDocumentDate(),
                document.getDescription(),
                message
        );
    }

    /**
     * Construye la respuesta de eliminación lógica (DOC-06 / HU-14).
     */
    public static DeleteDocumentResponse toDeleteResponse(Document document, String message) {
        Objects.requireNonNull(document, "document no puede ser null");
        return new DeleteDocumentResponse(document.getId(), message);
    }
}
