package co.edu.docurural.document.controller;

import co.edu.docurural.document.dto.BatchUploadDocumentResponseDto;
import co.edu.docurural.document.dto.BatchUploadItemResultDto;
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
import co.edu.docurural.shared.exception.GlobalExceptionHandler;
import co.edu.docurural.shared.util.ContentDispositionResolver;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
class DocumentBatchControllerWebMvcTest {

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
    void uploadBatch_returns200WithAllSuccessful_whenAllFilesValid() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        BatchUploadDocumentResponseDto response = new BatchUploadDocumentResponseDto(
                2, 2, 0,
                List.of(
                        new BatchUploadItemResultDto("acta_enero.pdf", true, 48L, null),
                        new BatchUploadItemResultDto("acta_febrero.pdf", true, 49L, null)));

        when(documentBatchService.uploadBatch(any(), any(), any())).thenReturn(response);

        MockMultipartFile file1 = new MockMultipartFile("files", "acta_enero.pdf", "application/pdf", new byte[100]);
        MockMultipartFile file2 = new MockMultipartFile("files", "acta_febrero.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/documents/batch")
                        .file(file1)
                        .file(file2)
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL")
                        .param("titles", "Acta Enero 2026")
                        .param("titles", "Acta Febrero 2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceived").value(2))
                .andExpect(jsonPath("$.totalSuccessful").value(2))
                .andExpect(jsonPath("$.totalFailed").value(0))
                .andExpect(jsonPath("$.results[0].fileName").value("acta_enero.pdf"))
                .andExpect(jsonPath("$.results[0].success").value(true))
                .andExpect(jsonPath("$.results[0].documentId").value(48))
                .andExpect(jsonPath("$.results[1].documentId").value(49));
    }

    @Test
    void uploadBatch_returns200WithMixedResults_whenOneFileFails() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        BatchUploadDocumentResponseDto response = new BatchUploadDocumentResponseDto(
                2, 1, 1,
                List.of(
                        new BatchUploadItemResultDto("bueno.pdf", true, 48L, null),
                        new BatchUploadItemResultDto("malo.pdf", false, null, "El archivo supera el tamaño máximo")));

        when(documentBatchService.uploadBatch(any(), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/documents/batch")
                        .file(new MockMultipartFile("files", "bueno.pdf", "application/pdf", new byte[100]))
                        .file(new MockMultipartFile("files", "malo.pdf", "application/pdf", new byte[100]))
                        .param("categoryId", "1")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSuccessful").value(1))
                .andExpect(jsonPath("$.totalFailed").value(1))
                .andExpect(jsonPath("$.results[1].success").value(false))
                .andExpect(jsonPath("$.results[1].errorMessage").value("El archivo supera el tamaño máximo"));
    }

    @Test
    void uploadBatch_returns400_whenMoreThanMaxFilesAllowed() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentBatchService.uploadBatch(any(), any(), any()))
                .thenThrow(new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, "Solo puede cargar hasta 5 archivos"));

        mockMvc.perform(multipart("/documents/batch")
                        .file(new MockMultipartFile("files", "f1.pdf", "application/pdf", new byte[10]))
                        .param("categoryId", "6")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadBatch_returns400_whenCategoryIdIsMissing() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        mockMvc.perform(multipart("/documents/batch")
                        .file(new MockMultipartFile("files", "doc.pdf", "application/pdf", new byte[10]))
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());
    }

    @Test
    void uploadBatch_returns400_whenResponsibleAreaIsBlank() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);

        mockMvc.perform(multipart("/documents/batch")
                        .file(new MockMultipartFile("files", "doc.pdf", "application/pdf", new byte[10]))
                        .param("categoryId", "1")
                        .param("responsibleArea", "")
                        .param("documentDate", "2026-03-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.responsibleArea").exists());
    }

    @Test
    void uploadBatch_returns404_whenCategoryNotFoundOrInactive() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(EDITOR_AUDIT);
        when(documentBatchService.uploadBatch(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("La categoría indicada no existe o está inactiva"));

        mockMvc.perform(multipart("/documents/batch")
                        .file(new MockMultipartFile("files", "doc.pdf", "application/pdf", new byte[10]))
                        .param("categoryId", "99")
                        .param("responsibleArea", "Rectoría")
                        .param("documentDate", "2026-03-15")
                        .param("sensitivityLevel", "INTERNAL"))
                .andExpect(status().isNotFound());
    }
}
