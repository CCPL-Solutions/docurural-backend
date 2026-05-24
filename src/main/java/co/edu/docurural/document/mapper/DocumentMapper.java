package co.edu.docurural.document.mapper;

import co.edu.docurural.document.dto.ActiveFiltersResponse;
import co.edu.docurural.document.dto.DeleteDocumentResponse;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.dto.DocumentSummaryResponse;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponse;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public abstract class DocumentMapper {

    @BeforeMapping
    protected void requireNonNull(Document document) {
        Objects.requireNonNull(document, "document no puede ser null");
    }

    @Mapping(source = "document.category.name", target = "category")
    @Mapping(target = "fileFormat",
            expression = "java(document.getFileFormat() != null ? document.getFileFormat().name() : null)")
    public abstract UploadDocumentResponse toUploadResponse(Document document, String message);

    @Mapping(source = "category.id", target = "category.id")
    @Mapping(source = "category.name", target = "category.name")
    @Mapping(source = "uploadedBy.id", target = "uploadedBy.id")
    @Mapping(source = "uploadedBy.fullName", target = "uploadedBy.fullName")
    @Mapping(target = "fileFormat",
            expression = "java(document.getFileFormat() != null ? document.getFileFormat().name() : null)")
    public abstract DocumentDetailResponse toDetailResponse(Document document);

    @Mapping(source = "document.category.name", target = "category")
    public abstract UpdateDocumentMetadataResponse toUpdateMetadataResponse(Document document, String message);

    public abstract DeleteDocumentResponse toDeleteResponse(Document document, String message);

    public DocumentListResponse toListResponse(Page<Document> page, int requestedPage, int requestedSize) {
        return toListResponse(page, requestedPage, requestedSize, null, null);
    }

    public DocumentListResponse toListResponse(
            Page<Document> page, int requestedPage, int requestedSize,
            String searchTerm, ActiveFiltersResponse activeFilters) {
        Objects.requireNonNull(page, "page no puede ser null");

        List<DocumentSummaryResponse> documents = page.getContent().stream()
                .map(this::toSummaryResponse)
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

    @Mapping(source = "category.name", target = "category")
    @Mapping(source = "uploadedBy.fullName", target = "uploadedBy")
    protected abstract DocumentSummaryResponse toSummaryResponse(Document document);
}
