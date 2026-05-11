package co.edu.docurural.document.controller;

import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentViewContent;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.service.DocumentBatchService;
import co.edu.docurural.document.service.DocumentService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.audit.AuditContextResolver;
import co.edu.docurural.shared.config.SecurityConfig;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.exception.GlobalExceptionHandler;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DocumentController.class,
        properties = "server.servlet.context-path=",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DocumentControllerWebMvcTest {

    private static final AuditContext EDITOR_AUDIT = new AuditContext(5L, "127.0.0.1");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DocumentService documentService;
    @MockitoBean
    DocumentBatchService documentBatchService;
    @MockitoBean
    AuditContextResolver auditContextResolver;

    @Test
    void upload_returns201AndPayload_whenValid() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        UploadDocumentResponse response = new UploadDocumentResponse(
                48L,
                "Acta Consejo Directivo Marzo 2026",
                "Actas",
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "PDF",
                524288L,
                "acta.pdf",
                LocalDateTime.of(2026, 4, 17, 10, 20),
                "Documento cargado exitosamente");

        when(documentService.upload(any(), any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta Consejo Directivo Marzo 2026")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(48))
                .andExpect(jsonPath("$.title").value("Acta Consejo Directivo Marzo 2026"))
                .andExpect(jsonPath("$.category").value("Actas"))
                .andExpect(jsonPath("$.fileFormat").value("PDF"))
                .andExpect(jsonPath("$.originalFileName").value("acta.pdf"))
                .andExpect(jsonPath("$.message").value("Documento cargado exitosamente"));
    }

    @Test
    void upload_returns400_whenTitleIsBlank() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void upload_returns400_whenCategoryIdIsMissing() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());
    }

    @Test
    void upload_returns404_whenCategoryNotFoundOrInactive() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentService.upload(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("document.category.not-found"));

        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("categoryId", "99")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_returns413_whenFileTooLarge() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentService.upload(any(), any(), any()))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.PAYLOAD_TOO_LARGE, "too large"));

        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void upload_returns415_whenMimeNotAllowed() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentService.upload(any(), any(), any()))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.UNSUPPORTED_MEDIA_TYPE, "bad mime"));

        MockMultipartFile file = new MockMultipartFile("file", "hack.txt", "text/plain", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void upload_returns500WithContractualMessage_whenStorageFails() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentService.upload(any(), any(), any()))
                .thenThrow(new FileStorageException("No se pudo almacenar el archivo en el servidor"));

        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("No se pudo almacenar el archivo en el servidor"));
    }

    @Test
    void upload_returns400WithFieldError_whenFileMissing() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        // Petición multipart sin el part "file" -> MissingServletRequestPartException -> 400
        mockMvc.perform(multipart("/documents")
                        .param("title", "Acta")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.file").exists());
    }

    // ------------------------------------------------------------------
    // GET /documents/{id} (DOC-02)
    // ------------------------------------------------------------------

    @Test
    void getById_returns200WithDetail_whenDocumentActive() throws Exception {
        DocumentDetailResponse response = new DocumentDetailResponse(
                48L,
                "Acta Consejo Directivo Marzo 2026",
                "Acta de reunión",
                new DocumentDetailResponse.CategoryRef(1L, "Actas"),
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "PDF",
                524288L,
                "acta.pdf",
                new DocumentDetailResponse.UploadedByRef(10L, "Ana Admin"),
                LocalDateTime.of(2026, 4, 10, 9, 30));

        when(documentService.findDetailById(48L)).thenReturn(response);

        mockMvc.perform(get("/documents/48"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(48))
                .andExpect(jsonPath("$.title").value("Acta Consejo Directivo Marzo 2026"))
                .andExpect(jsonPath("$.category.id").value(1))
                .andExpect(jsonPath("$.category.name").value("Actas"))
                .andExpect(jsonPath("$.uploadedBy.id").value(10))
                .andExpect(jsonPath("$.uploadedBy.fullName").value("Ana Admin"))
                .andExpect(jsonPath("$.fileFormat").value("PDF"));
    }

    @Test
    void getById_returns404_whenDocumentNotFound() throws Exception {
        when(documentService.findDetailById(99L))
                .thenThrow(new ResourceNotFoundException("document.not-found"));

        mockMvc.perform(get("/documents/99"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // GET /documents/{id}/view (DOC-07)
    // ------------------------------------------------------------------

    @Test
    void view_returns200WithInlineDispositionAndStream_whenPdf() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        byte[] pdfBytes = "%PDF-1.4 test".getBytes();
        DocumentViewContent content = new DocumentViewContent(
                new ByteArrayResource(pdfBytes), DocumentFormat.PDF, "acta.pdf", pdfBytes.length);

        when(documentService.openForView(eq(48L), any())).thenReturn(content);

        mockMvc.perform(get("/documents/48/view"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(header().string("X-File-Name", "acta.pdf"))
                .andExpect(header().string("X-File-Size", String.valueOf(pdfBytes.length)))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void view_returns200WithAttachmentDisposition_whenDocx() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        byte[] docxBytes = new byte[]{1, 2, 3};
        DocumentViewContent content = new DocumentViewContent(
                new ByteArrayResource(docxBytes), DocumentFormat.DOCX, "informe.docx", docxBytes.length);

        when(documentService.openForView(eq(48L), any())).thenReturn(content);

        mockMvc.perform(get("/documents/48/view"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment")))
                .andExpect(header().string("Content-Type", startsWith(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document")));
    }

    @Test
    void view_returns404_whenDocumentNotFound() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentService.openForView(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("document.not-found"));

        mockMvc.perform(get("/documents/99/view"))
                .andExpect(status().isNotFound());
    }

    @Test
    void view_returns404_whenFileMissingOnDisk() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentService.openForView(eq(48L), any()))
                .thenThrow(new ResourceNotFoundException("document.file.not-available"));

        mockMvc.perform(get("/documents/48/view"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("document.file.not-available"));
    }

    @Test
    void view_returns500_whenFileStorageFails() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentService.openForView(eq(48L), any()))
                .thenThrow(new FileStorageException("document.file.not-available"));

        mockMvc.perform(get("/documents/48/view"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("document.file.not-available"));
    }
}
