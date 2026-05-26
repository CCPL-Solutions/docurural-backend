package co.edu.docurural.document.mapper;

import co.edu.docurural.document.dto.ActiveFiltersResponseDto;
import co.edu.docurural.document.dto.DeleteDocumentResponseDto;
import co.edu.docurural.document.dto.DocumentDetailResponseDto;
import co.edu.docurural.document.dto.DocumentListResponseDto;
import co.edu.docurural.document.dto.DocumentSummaryResponseDto;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponseDto;
import co.edu.docurural.document.dto.UploadDocumentResponseDto;
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
    public abstract UploadDocumentResponseDto toUploadResponse(Document document, String message);

    @Mapping(source = "category.id", target = "category.id")
    @Mapping(source = "category.name", target = "category.name")
    @Mapping(source = "uploadedBy.id", target = "uploadedBy.id")
    @Mapping(source = "uploadedBy.fullName", target = "uploadedBy.fullName")
    @Mapping(target = "fileFormat",
            expression = "java(document.getFileFormat() != null ? document.getFileFormat().name() : null)")
    public abstract DocumentDetailResponseDto toDetailResponse(Document document);

    @Mapping(source = "document.category.name", target = "category")
    public abstract UpdateDocumentMetadataResponseDto toUpdateMetadataResponse(Document document, String message);

    public abstract DeleteDocumentResponseDto toDeleteResponse(Document document, String message);

    public DocumentListResponseDto toListResponse(Page<Document> page, int requestedPage, int requestedSize) {
        return toListResponse(page, requestedPage, requestedSize, null, null);
    }

    public DocumentListResponseDto toListResponse(
            Page<Document> page, int requestedPage, int requestedSize,
            String searchTerm, ActiveFiltersResponseDto activeFilters) {
        Objects.requireNonNull(page, "page no puede ser null");

        List<DocumentSummaryResponseDto> documents = page.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new DocumentListResponseDto(
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
    protected abstract DocumentSummaryResponseDto toSummaryResponse(Document document);
}
