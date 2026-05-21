package co.edu.docurural.dashboard.controller;

import co.edu.docurural.dashboard.dto.CategoryDistributionItemResponse;
import co.edu.docurural.dashboard.dto.DashboardStatsResponse;
import co.edu.docurural.dashboard.dto.RecentDocumentResponse;
import co.edu.docurural.dashboard.dto.SummaryResponse;
import co.edu.docurural.dashboard.dto.TopCategoryResponse;
import co.edu.docurural.dashboard.service.DashboardService;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.shared.config.SecurityConfig;
import co.edu.docurural.shared.exception.GlobalExceptionHandler;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DashboardController.class,
        properties = "server.servlet.context-path=",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DashboardService dashboardService;

    // -------------------------------------------------------------------------
    // GET /dashboard/stats — respuesta con datos
    // -------------------------------------------------------------------------

    @Test
    void getStats_returns200WithBody_whenServiceReturnsData() throws Exception {
        DashboardStatsResponse response = buildFilledResponse();
        when(dashboardService.getStats()).thenReturn(response);

        mockMvc.perform(get("/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalActiveDocuments").value(47))
                .andExpect(jsonPath("$.summary.documentsUploadedThisMonth").value(12))
                .andExpect(jsonPath("$.summary.topCategory.name").value("Actas"))
                .andExpect(jsonPath("$.summary.topCategory.count").value(18))
                .andExpect(jsonPath("$.categoryDistribution[0].categoryName").value("Actas"))
                .andExpect(jsonPath("$.categoryDistribution[0].count").value(18))
                .andExpect(jsonPath("$.categoryDistribution[0].percentage").value(38.30))
                .andExpect(jsonPath("$.recentDocuments[0].id").value(47))
                .andExpect(jsonPath("$.recentDocuments[0].title").value("Acta Consejo Directivo Mayo 2026"))
                .andExpect(jsonPath("$.recentDocuments[0].category").value("Actas"))
                .andExpect(jsonPath("$.recentDocuments[0].fileFormat").value("PDF"));
    }

    // -------------------------------------------------------------------------
    // GET /dashboard/stats — repositorio vacío
    // -------------------------------------------------------------------------

    @Test
    void getStats_returns200WithEmptyArraysAndNullTop_whenRepositoryIsEmpty() throws Exception {
        DashboardStatsResponse emptyResponse = new DashboardStatsResponse(
                new SummaryResponse(0L, 0L, null),
                List.of(),
                List.of());
        when(dashboardService.getStats()).thenReturn(emptyResponse);

        mockMvc.perform(get("/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalActiveDocuments").value(0))
                .andExpect(jsonPath("$.summary.documentsUploadedThisMonth").value(0))
                .andExpect(jsonPath("$.summary.topCategory").isEmpty())
                .andExpect(jsonPath("$.categoryDistribution").isArray())
                .andExpect(jsonPath("$.categoryDistribution").isEmpty())
                .andExpect(jsonPath("$.recentDocuments").isArray())
                .andExpect(jsonPath("$.recentDocuments").isEmpty());
    }

    // -------------------------------------------------------------------------
    // GET /dashboard/stats — error interno propagado con formato estándar
    // -------------------------------------------------------------------------

    @Test
    void getStats_returns500WithApiErrorFormat_whenServiceThrowsUnexpectedException() throws Exception {
        when(dashboardService.getStats()).thenThrow(new RuntimeException("error inesperado"));

        mockMvc.perform(get("/dashboard/stats"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static DashboardStatsResponse buildFilledResponse() {
        SummaryResponse summary = new SummaryResponse(47L, 12L,
                new TopCategoryResponse("Actas", 18L));

        List<CategoryDistributionItemResponse> distribution = List.of(
                new CategoryDistributionItemResponse("Actas", 18L, 38.30),
                new CategoryDistributionItemResponse("Circulares", 11L, 23.40));

        List<RecentDocumentResponse> recent = List.of(
                new RecentDocumentResponse(
                        47L,
                        "Acta Consejo Directivo Mayo 2026",
                        "Actas",
                        "Rectoría",
                        DocumentFormat.PDF,
                        LocalDateTime.of(2026, 5, 15, 8, 45)));

        return new DashboardStatsResponse(summary, distribution, recent);
    }
}
