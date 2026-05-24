package co.edu.docurural.document.mapper;

import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponse;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentMapperTest {

    private final DocumentMapper mapper = Mappers.getMapper(DocumentMapper.class);

    @Test
    void toUploadResponse_mapsAllFieldsCorrectly() {
        var category = TestFixtures.categoryActive(1L, "Actas");
        var user = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(48L, category, user);

        UploadDocumentResponse response = mapper.toUploadResponse(doc, "Documento cargado exitosamente");

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.title()).isEqualTo("Acta Consejo Directivo Marzo 2026");
        assertThat(response.category()).isEqualTo("Actas");
        assertThat(response.responsibleArea()).isEqualTo("Rectoría");
        assertThat(response.fileFormat()).isEqualTo(DocumentFormat.PDF.name());
        assertThat(response.fileSizeBytes()).isEqualTo(524288L);
        assertThat(response.originalFileName()).isEqualTo("acta.pdf");
        assertThat(response.createdAt()).isEqualTo(TestFixtures.FIXED_CREATED_AT);
        assertThat(response.message()).isEqualTo("Documento cargado exitosamente");
    }

    @Test
    void toUploadResponse_nullFileFormat_returnsNullFormatField() {
        var category = TestFixtures.categoryActive(1L, "Actas");
        var user = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(1L, category, user);
        doc.setFileFormat(null);

        UploadDocumentResponse response = mapper.toUploadResponse(doc, "ok");

        assertThat(response.fileFormat()).isNull();
    }

    @Test
    void toUploadResponse_throwsOnNullDocument() {
        assertThatThrownBy(() -> mapper.toUploadResponse(null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }

    // ------------------------------------------------------------------
    // toDetailResponse()
    // ------------------------------------------------------------------

    @Test
    void toDetailResponse_mapsAllFields_whenDocumentValid() {
        var category = TestFixtures.categoryActive(1L, "Actas", "Actas de reuniones");
        var user = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(48L, category, user);

        DocumentDetailResponse response = mapper.toDetailResponse(doc);

        assertThat(response.id()).isEqualTo(48L);
        assertThat(response.title()).isEqualTo(doc.getTitle());
        assertThat(response.description()).isEqualTo(doc.getDescription());
        assertThat(response.category().id()).isEqualTo(1L);
        assertThat(response.category().name()).isEqualTo("Actas");
        assertThat(response.responsibleArea()).isEqualTo(doc.getResponsibleArea());
        assertThat(response.documentDate()).isEqualTo(doc.getDocumentDate());
        assertThat(response.fileFormat()).isEqualTo(DocumentFormat.PDF.name());
        assertThat(response.fileSizeBytes()).isEqualTo(doc.getFileSizeBytes());
        assertThat(response.originalFileName()).isEqualTo(doc.getOriginalFileName());
        assertThat(response.uploadedBy().id()).isEqualTo(10L);
        assertThat(response.uploadedBy().fullName()).isEqualTo(user.getFullName());
        assertThat(response.createdAt()).isEqualTo(TestFixtures.FIXED_CREATED_AT);
    }

    @Test
    void toDetailResponse_mapsDescriptionAsNull_whenAbsent() {
        var category = TestFixtures.categoryActive(1L, "Actas");
        var user = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(48L, category, user);
        doc.setDescription(null);

        DocumentDetailResponse response = mapper.toDetailResponse(doc);

        assertThat(response.description()).isNull();
    }

    @Test
    void toDetailResponse_throwsOnNullDocument() {
        assertThatThrownBy(() -> mapper.toDetailResponse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toUpdateMetadataResponse_mapsAllFieldsCorrectly() {
        var category = TestFixtures.categoryActive(1L, "Actas");
        var user = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(47L, category, user);
        doc.setTitle("Acta Consejo Directivo Marzo 2026 - Revisado");

        UpdateDocumentMetadataResponse response =
                mapper.toUpdateMetadataResponse(doc, "Documento actualizado exitosamente");

        assertThat(response.id()).isEqualTo(47L);
        assertThat(response.title()).isEqualTo("Acta Consejo Directivo Marzo 2026 - Revisado");
        assertThat(response.category()).isEqualTo("Actas");
        assertThat(response.responsibleArea()).isEqualTo("Rectoría");
        assertThat(response.documentDate()).isEqualTo(doc.getDocumentDate());
        assertThat(response.message()).isEqualTo("Documento actualizado exitosamente");
    }

    @Test
    void toUpdateMetadataResponse_throwsOnNullDocument() {
        assertThatThrownBy(() -> mapper.toUpdateMetadataResponse(null, "ok"))
                .isInstanceOf(NullPointerException.class);
    }

    // ------------------------------------------------------------------
    // toListResponse()
    // ------------------------------------------------------------------

    @Test
    void toListResponse_mapsAllFieldsCorrectly() {
        var category = TestFixtures.categoryActive(1L, "Actas");
        var user = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(47L, category, user);

        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> page = new PageImpl<>(List.of(doc), pageRequest, 1L);

        DocumentListResponse response = mapper.toListResponse(page, 1, 20);

        assertThat(response.totalDocuments()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.documents()).hasSize(1);

        var summary = response.documents().get(0);
        assertThat(summary.id()).isEqualTo(47L);
        assertThat(summary.title()).isEqualTo(doc.getTitle());
        assertThat(summary.category()).isEqualTo("Actas");
        assertThat(summary.responsibleArea()).isEqualTo(doc.getResponsibleArea());
        assertThat(summary.documentDate()).isEqualTo(doc.getDocumentDate());
        assertThat(summary.fileFormat()).isEqualTo(DocumentFormat.PDF);
        assertThat(summary.fileSizeBytes()).isEqualTo(doc.getFileSizeBytes());
        assertThat(summary.uploadedBy()).isEqualTo(user.getFullName());
        assertThat(summary.createdAt()).isEqualTo(TestFixtures.FIXED_CREATED_AT);
    }

    @Test
    void toListResponse_returnsEmptyListAndZeroTotals_whenPageIsEmpty() {
        Page<Document> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L);

        DocumentListResponse response = mapper.toListResponse(page, 1, 20);

        assertThat(response.totalDocuments()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.documents()).isEmpty();
    }

    @Test
    void toListResponse_throwsOnNullPage() {
        assertThatThrownBy(() -> mapper.toListResponse(null, 1, 20))
                .isInstanceOf(NullPointerException.class);
    }
}
