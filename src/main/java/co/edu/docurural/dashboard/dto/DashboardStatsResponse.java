package co.edu.docurural.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Respuesta completa del endpoint DSH-01 {@code GET /api/dashboard/stats}.
 * Agrega en una sola llamada todos los datos necesarios para renderizar el
 * panel de control: tarjetas de resumen (HU-24), últimos documentos (HU-25)
 * y distribución por categoría (HU-26).
 */
@Schema(description = "Datos completos del panel de control")
public record DashboardStatsResponse(

        @Schema(description = "Tarjetas de resumen estadístico del repositorio")
        SummaryResponse summary,

        @Schema(description = "Distribución de documentos activos por categoría para el gráfico. "
                + "Solo incluye categorías con al menos 1 documento activo.")
        List<CategoryDistributionItemResponse> categoryDistribution,

        @Schema(description = "Últimos 10 documentos cargados con status ACTIVE, ordenados por fecha de carga DESC.")
        List<RecentDocumentResponse> recentDocuments
) {
}
