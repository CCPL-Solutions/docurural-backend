package co.edu.docurural.document.mapper;

import co.edu.docurural.document.dto.ActiveFiltersResponse;
import co.edu.docurural.document.dto.DeleteDocumentResponse;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.dto.DocumentSummaryResponse;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponse;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import org.springframework.data.domain.Page;

import java.util.List;
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

    /**
     * Construye la respuesta paginada del listado de documentos activos (DOC-01 / HU-15).
     *
     * @param page          resultado de Spring Data con los documentos de la página solicitada
     * @param requestedPage número de página 1-based pedido por el cliente
     * @param requestedSize tamaño de página pedido por el cliente
     */
    public static DocumentListResponse toListResponse(Page<Document> page, int requestedPage, int requestedSize) {
        return toListResponse(page, requestedPage, requestedSize, null, null);
    }

    /**
     * Construye la respuesta paginada de búsqueda y filtrado de documentos (SRC-01 / HU-20, HU-21, HU-22).
     *
     * @param page          resultado de Spring Data con los documentos de la página solicitada
     * @param requestedPage número de página 1-based pedido por el cliente
     * @param requestedSize tamaño de página pedido por el cliente
     * @param searchTerm    término de búsqueda por texto aplicado; {@code null} si no hubo búsqueda
     * @param activeFilters filtros estructurados activos; {@code null} si no hubo filtros
     */
    public static DocumentListResponse toListResponse(
            Page<Document> page, int requestedPage, int requestedSize,
            String searchTerm, ActiveFiltersResponse activeFilters) {
        Objects.requireNonNull(page, "page no puede ser null");

        List<DocumentSummaryResponse> documents = page.getContent().stream()
                .map(DocumentMapper::toSummaryResponse)
                .toList();

        return new DocumentListResponse(
                (int) page.getTotalElements(),
                page.getTotalPages(),
                requestedPage,
                requestedSize,
                searchTerm,
                activeFilters,
                documents
        );
    }

    private static DocumentSummaryResponse toSummaryResponse(Document document) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getTitle(),
                document.getCategory().getName(),
                document.getResponsibleArea(),
                document.getDocumentDate(),
                document.getFileFormat(),
                document.getFileSizeBytes(),
                document.getUploadedBy().getFullName(),
                document.getCreatedAt()
        );
    }
}
