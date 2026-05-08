package co.edu.docurural.document.mapper;

import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentMapperTest {

    @Test
    void toUploadResponse_mapsAllFieldsCorrectly() {
        var category = TestFixtures.categoryActive(1L, "Actas");
        var user = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(48L, category, user);

        UploadDocumentResponse response = DocumentMapper.toUploadResponse(doc, "Documento cargado exitosamente");

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

        UploadDocumentResponse response = DocumentMapper.toUploadResponse(doc, "ok");

        assertThat(response.fileFormat()).isNull();
    }

    @Test
    void toUploadResponse_throwsOnNullDocument() {
        assertThatThrownBy(() -> DocumentMapper.toUploadResponse(null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }
}
