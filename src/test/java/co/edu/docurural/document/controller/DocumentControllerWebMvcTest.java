package co.edu.docurural.document.controller;

import co.edu.docurural.document.dto.ActiveFiltersResponseDto;
import co.edu.docurural.document.dto.DeleteDocumentResponseDto;
import co.edu.docurural.document.dto.DocumentDetailResponseDto;
import co.edu.docurural.document.dto.DocumentFileContentDto;
import co.edu.docurural.document.dto.DocumentListResponseDto;
import co.edu.docurural.document.dto.DocumentSummaryResponseDto;
import co.edu.docurural.document.dto.FilterOptionsResponseDto;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponseDto;
import co.edu.docurural.document.dto.UploadDocumentResponseDto;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.service.DocumentBatchService;
import co.edu.docurural.document.service.DocumentCommandService;
import co.edu.docurural.document.service.DocumentContentService;
import co.edu.docurural.document.service.DocumentQueryService;
import co.edu.docurural.document.service.DocumentSearchService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.audit.AuditContextResolver;
import co.edu.docurural.shared.config.SecurityConfig;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.exception.GlobalExceptionHandler;
import co.edu.docurural.shared.util.ContentDispositionResolver;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
@Import({GlobalExceptionHandler.class, ContentDispositionResolver.class})
class DocumentControllerWebMvcTest {

    private static final AuditContext EDITOR_AUDIT = new AuditContext(5L, "127.0.0.1");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DocumentCommandService documentCommandService;
    @MockitoBean
    DocumentQueryService documentQueryService;
    @MockitoBean
    DocumentSearchService documentSearchService;
    @MockitoBean
    DocumentContentService documentContentService;
    @MockitoBean
    DocumentBatchService documentBatchService;
    @MockitoBean
    AuditContextResolver auditContextResolver;

    @Test
    void upload_returns201AndPayload_whenValid() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        UploadDocumentResponseDto response = new UploadDocumentResponseDto(
                48L,
                "Acta Consejo Directivo Marzo 2026",
                "Actas",
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "PDF",
                524288L,
                "acta.pdf",
                LocalDateTime.of(2026, 4, 17, 10, 20),
                "INTERNAL",
                "Documento cargado exitosamente");

        when(documentCommandService.upload(any(), any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta Consejo Directivo Marzo 2026")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL"))
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
        when(documentCommandService.upload(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("document.category.not-found"));

        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("categoryId", "99")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL"))
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_returns413_whenFileTooLarge() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentCommandService.upload(any(), any(), any()))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.PAYLOAD_TOO_LARGE, "too large"));

        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL"))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void upload_returns415_whenMimeNotAllowed() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentCommandService.upload(any(), any(), any()))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.UNSUPPORTED_MEDIA_TYPE, "bad mime"));

        MockMultipartFile file = new MockMultipartFile("file", "hack.txt", "text/plain", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void upload_returns500WithContractualMessage_whenStorageFails() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentCommandService.upload(any(), any(), any()))
                .thenThrow(new FileStorageException("No se pudo almacenar el archivo en el servidor"));

        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("title", "Acta")
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL"))
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
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.file").exists());
    }

    // ------------------------------------------------------------------
    // PUT /documents/{id} (DOC-05)
    // ------------------------------------------------------------------

    @Test
    void updateMetadata_returns200AndPayload_whenValid() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        UpdateDocumentMetadataResponseDto response = new UpdateDocumentMetadataResponseDto(
                47L,
                "Acta Consejo Directivo Marzo 2026 - Revisado",
                "Actas",
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "Descripción",
                "INTERNAL",
                "Documento actualizado exitosamente");

        when(documentCommandService.updateMetadata(eq(47L), any(), any())).thenReturn(response);

        mockMvc.perform(put("/documents/47")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Acta Consejo Directivo Marzo 2026 - Revisado",
                                  "categoryId": 1,
                                  "responsibleArea": "Rectoría",
                                  "documentDate": "2026-03-15",
                                  "description": "Versión corregida del acta",
                                  "sensitivityLevel": "INTERNAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(47))
                .andExpect(jsonPath("$.title").value("Acta Consejo Directivo Marzo 2026 - Revisado"))
                .andExpect(jsonPath("$.category").value("Actas"))
                .andExpect(jsonPath("$.message").value("Documento actualizado exitosamente"));
    }

    @Test
    void updateMetadata_returns400_whenTitleIsBlank() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        mockMvc.perform(put("/documents/47")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "",
                                  "categoryId": 1,
                                  "responsibleArea": "Rectoría",
                                  "documentDate": "2026-03-15"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void updateMetadata_returns403_whenEditorTriesToEditForeignDocument() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentCommandService.updateMetadata(eq(47L), any(), any()))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.FORBIDDEN,
                        "No tiene permisos para editar este documento"));

        mockMvc.perform(put("/documents/47")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Acta Consejo Directivo Marzo 2026 - Revisado",
                                  "categoryId": 1,
                                  "responsibleArea": "Rectoría",
                                  "documentDate": "2026-03-15",
                                  "sensitivityLevel": "INTERNAL"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para editar este documento"));
    }

    // ------------------------------------------------------------------
    // DELETE /documents/{id} (DOC-06)
    // ------------------------------------------------------------------

    @Test
    void deleteLogical_returns200AndPayload_whenDocumentActive() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        DeleteDocumentResponseDto response = new DeleteDocumentResponseDto(
                47L,
                "Documento eliminado exitosamente");

        when(documentCommandService.deleteLogical(eq(47L), any())).thenReturn(response);

        mockMvc.perform(delete("/documents/47"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(47))
                .andExpect(jsonPath("$.message").value("Documento eliminado exitosamente"));
    }

    @Test
    void deleteLogical_returns404_whenDocumentNotFoundOrAlreadyDeleted() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentCommandService.deleteLogical(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("document.not-found"));

        mockMvc.perform(delete("/documents/99"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // GET /documents/{id} (DOC-02)
    // ------------------------------------------------------------------

    @Test
    void getById_returns200WithDetail_whenDocumentActive() throws Exception {
        DocumentDetailResponseDto response = new DocumentDetailResponseDto(
                48L,
                "Acta Consejo Directivo Marzo 2026",
                "Acta de reunión",
                new DocumentDetailResponseDto.CategoryRef(1L, "Actas"),
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                "PDF",
                524288L,
                "acta.pdf",
                new DocumentDetailResponseDto.UploadedByRef(10L, "Ana Admin"),
                LocalDateTime.of(2026, 4, 10, 9, 30),
                "3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7",
                "INTERNAL");

        when(documentQueryService.findDetailById(eq(48L), any())).thenReturn(response);

        mockMvc.perform(get("/documents/48"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(48))
                .andExpect(jsonPath("$.title").value("Acta Consejo Directivo Marzo 2026"))
                .andExpect(jsonPath("$.category.id").value(1))
                .andExpect(jsonPath("$.category.name").value("Actas"))
                .andExpect(jsonPath("$.uploadedBy.id").value(10))
                .andExpect(jsonPath("$.uploadedBy.fullName").value("Ana Admin"))
                .andExpect(jsonPath("$.fileHash").value("3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7"))
                .andExpect(jsonPath("$.fileFormat").value("PDF"));
    }

    @Test
    void getById_returns404_whenDocumentNotFound() throws Exception {
        when(documentQueryService.findDetailById(eq(99L), any()))
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
        DocumentFileContentDto content = new DocumentFileContentDto(
                new ByteArrayResource(pdfBytes), DocumentFormat.PDF, "acta.pdf", pdfBytes.length);

        when(documentContentService.openForView(eq(48L), any())).thenReturn(content);

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
        DocumentFileContentDto content = new DocumentFileContentDto(
                new ByteArrayResource(docxBytes), DocumentFormat.DOCX, "informe.docx", docxBytes.length);

        when(documentContentService.openForView(eq(48L), any())).thenReturn(content);

        mockMvc.perform(get("/documents/48/view"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment")))
                .andExpect(header().string("Content-Type", startsWith(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document")));
    }

    @Test
    void view_returns404_whenDocumentNotFound() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentContentService.openForView(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("document.not-found"));

        mockMvc.perform(get("/documents/99/view"))
                .andExpect(status().isNotFound());
    }

    @Test
    void view_returns404_whenFileMissingOnDisk() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentContentService.openForView(eq(48L), any()))
                .thenThrow(new ResourceNotFoundException("document.file.not-available"));

        mockMvc.perform(get("/documents/48/view"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("document.file.not-available"));
    }

    @Test
    void view_returns500_whenFileStorageFails() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentContentService.openForView(eq(48L), any()))
                .thenThrow(new FileStorageException("document.file.not-available"));

        mockMvc.perform(get("/documents/48/view"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("document.file.not-available"));
    }

    // ------------------------------------------------------------------
    // GET /documents/{id}/download (DOC-08)
    // ------------------------------------------------------------------

    @Test
    void download_returns200WithAttachmentDispositionAndStream_whenPdf() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        byte[] pdfBytes = "%PDF-1.4 test".getBytes();
        DocumentFileContentDto content = new DocumentFileContentDto(
                new ByteArrayResource(pdfBytes), DocumentFormat.PDF, "acta.pdf", pdfBytes.length);

        when(documentContentService.openForDownload(eq(48L), any())).thenReturn(content);

        mockMvc.perform(get("/documents/48/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")))
                .andExpect(header().string("X-File-Name", "acta.pdf"))
                .andExpect(header().string("X-File-Size", String.valueOf(pdfBytes.length)))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void download_returns200WithAttachmentDisposition_whenPng() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        DocumentFileContentDto content = new DocumentFileContentDto(
                new ByteArrayResource(pngBytes), DocumentFormat.PNG, "imagen.png", pngBytes.length);

        when(documentContentService.openForDownload(eq(48L), any())).thenReturn(content);

        mockMvc.perform(get("/documents/48/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")));
    }

    @Test
    void download_returns404_whenDocumentNotFound() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentContentService.openForDownload(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("document.not-found"));

        mockMvc.perform(get("/documents/99/download"))
                .andExpect(status().isNotFound());
    }

    @Test
    void download_returns404_whenFileMissingOnDisk() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentContentService.openForDownload(eq(48L), any()))
                .thenThrow(new ResourceNotFoundException("document.file.not-available"));

        mockMvc.perform(get("/documents/48/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("document.file.not-available"));
    }

    @Test
    void download_returns500_whenFileStorageFails() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentContentService.openForDownload(eq(48L), any()))
                .thenThrow(new FileStorageException("document.file.load-failed"));

        mockMvc.perform(get("/documents/48/download"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("document.file.load-failed"));
    }

    @Test
    void download_sanitizesControlCharsInXFileNameHeader() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        byte[] pdfBytes = new byte[]{1, 2, 3};
        DocumentFileContentDto content = new DocumentFileContentDto(
                new ByteArrayResource(pdfBytes), DocumentFormat.PDF, "malo\r\nInjected: bad", pdfBytes.length);

        when(documentContentService.openForDownload(eq(48L), any())).thenReturn(content);

        mockMvc.perform(get("/documents/48/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-File-Name", "maloInjected: bad"));
    }

    // ------------------------------------------------------------------
    // GET /documents (DOC-01 / SRC-01 — HU-15, HU-20, HU-21, HU-22)
    // ------------------------------------------------------------------

    @Test
    void list_returns200WithEnvelope_whenServiceReturnsDocuments() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        DocumentSummaryResponseDto summary = new DocumentSummaryResponseDto(
                47L,
                "Acta Consejo Directivo Marzo 2026",
                "Actas",
                "Rectoría",
                LocalDate.of(2026, 3, 15),
                DocumentFormat.PDF,
                524288L,
                "Ana Admin",
                LocalDateTime.of(2026, 4, 10, 9, 30),
                "INTERNAL");

        DocumentListResponseDto listResponse = new DocumentListResponseDto(47, 3, 1, 20, null, null, List.of(summary));
        when(documentSearchService.search(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), any())).thenReturn(listResponse);

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocuments").value(47))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.searchTerm").doesNotExist())
                .andExpect(jsonPath("$.documents[0].id").value(47))
                .andExpect(jsonPath("$.documents[0].title").value("Acta Consejo Directivo Marzo 2026"))
                .andExpect(jsonPath("$.documents[0].category").value("Actas"))
                .andExpect(jsonPath("$.documents[0].fileFormat").value("PDF"));
    }

    @Test
    void list_returns200WithSearchTermAndActiveFilters_whenQAndFiltersProvided() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        ActiveFiltersResponseDto filters = new ActiveFiltersResponseDto(3L, "Actas", null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31), null);
        DocumentListResponseDto listResponse = new DocumentListResponseDto(8, 1, 1, 20, "acta", filters, List.of());
        when(documentSearchService.search(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), any())).thenReturn(listResponse);

        mockMvc.perform(get("/documents")
                        .param("q", "acta")
                        .param("categoryId", "3")
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchTerm").value("acta"))
                .andExpect(jsonPath("$.activeFilters.categoryId").value(3))
                .andExpect(jsonPath("$.activeFilters.categoryName").value("Actas"))
                .andExpect(jsonPath("$.activeFilters.dateFrom").value("2026-01-01"))
                .andExpect(jsonPath("$.activeFilters.dateTo").value("2026-05-31"))
                .andExpect(jsonPath("$.totalDocuments").value(8));
    }

    @Test
    void list_passesAllParamsToService() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        DocumentListResponseDto listResponse = new DocumentListResponseDto(0, 0, 2, 10, null, null, List.of());
        when(documentSearchService.search(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), any())).thenReturn(listResponse);

        mockMvc.perform(get("/documents")
                        .param("q", "acta")
                        .param("categoryId", "3")
                        .param("responsibleArea", "Rectoría")
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-05-31")
                        .param("uploadedBy", "1")
                        .param("page", "2")
                        .param("size", "10")
                        .param("sortBy", "title")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> categoryIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> sortByCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sortDirCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentSearchService).search(
                qCaptor.capture(), categoryIdCaptor.capture(), any(), any(), any(), any(),
                pageCaptor.capture(), sizeCaptor.capture(), sortByCaptor.capture(), sortDirCaptor.capture(),
                eq(false), any());
        assertThat(qCaptor.getValue()).isEqualTo("acta");
        assertThat(categoryIdCaptor.getValue()).isEqualTo(3L);
        assertThat(pageCaptor.getValue()).isEqualTo(2);
        assertThat(sizeCaptor.getValue()).isEqualTo(10);
        assertThat(sortByCaptor.getValue()).isEqualTo("title");
        assertThat(sortDirCaptor.getValue()).isEqualTo("asc");
    }

    @Test
    void list_returns400_whenServiceThrowsInvalidArgumentForQ() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentSearchService.search(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), any()))
                .thenThrow(new BusinessRuleException(
                        BusinessErrorCode.INVALID_ARGUMENT,
                        "Ingrese al menos 2 caracteres para buscar"));

        mockMvc.perform(get("/documents").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ingrese al menos 2 caracteres para buscar"));
    }

    @Test
    void list_returns400_whenServiceThrowsInvalidArgumentForDateRange() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentSearchService.search(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), any()))
                .thenThrow(new BusinessRuleException(
                        BusinessErrorCode.INVALID_ARGUMENT,
                        "La fecha de inicio no puede ser posterior a la fecha de fin"));

        mockMvc.perform(get("/documents")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-04-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "La fecha de inicio no puede ser posterior a la fecha de fin"));
    }

    @Test
    void list_returns400_whenServiceThrowsInvalidArgumentForSort() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentSearchService.search(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), any()))
                .thenThrow(new BusinessRuleException(
                        BusinessErrorCode.INVALID_ARGUMENT,
                        "El campo de ordenamiento ''fileSize'' no es soportado. Use createdAt, title o documentDate"));

        mockMvc.perform(get("/documents").param("sortBy", "fileSize"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El campo de ordenamiento ''fileSize'' no es soportado. Use createdAt, title o documentDate"));
    }

    // ------------------------------------------------------------------
    // GET /documents/filter-options (SRC-02 — HU-21)
    // ------------------------------------------------------------------

    @Test
    void filterOptions_returns200WithCategoriesAndUsers_whenAdmin() throws Exception {
        FilterOptionsResponseDto response = new FilterOptionsResponseDto(
                List.of(new FilterOptionsResponseDto.CategoryOption(1L, "Actas"),
                        new FilterOptionsResponseDto.CategoryOption(2L, "Circulares")),
                List.of(new FilterOptionsResponseDto.UserOption(10L, "Ana Admin")));
        when(documentSearchService.getFilterOptions(false)).thenReturn(response);

        mockMvc.perform(get("/documents/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories[0].id").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("Actas"))
                .andExpect(jsonPath("$.users[0].id").value(10))
                .andExpect(jsonPath("$.users[0].fullName").value("Ana Admin"));
    }

    @Test
    void filterOptions_returns200WithCategoriesAndNullUsers_whenEditor() throws Exception {
        FilterOptionsResponseDto response = new FilterOptionsResponseDto(
                List.of(new FilterOptionsResponseDto.CategoryOption(1L, "Actas")),
                null);
        when(documentSearchService.getFilterOptions(false)).thenReturn(response);

        mockMvc.perform(get("/documents/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.users").doesNotExist());
    }
}
