package co.edu.docurural.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tarjetas de resumen estadístico del repositorio (HU-24).
 */
@Schema(description = "Tarjetas de resumen estadístico del repositorio")
public record SummaryResponseDto(

        @Schema(description = "Total de documentos con status ACTIVE en el repositorio", example = "47")
        long totalActiveDocuments,

        @Schema(description = "Documentos cargados en el mes en curso", example = "12")
        long documentsUploadedThisMonth,

        @Schema(description = "Categoría con más documentos activos. Null si el repositorio está vacío.", nullable = true)
        TopCategoryResponseDto topCategory
) {
}
