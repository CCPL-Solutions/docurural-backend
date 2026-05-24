package co.edu.docurural.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Elemento de la distribución de documentos por categoría para el gráfico del dashboard (HU-26).
 * Solo incluye categorías con al menos 1 documento activo.
 */
@Schema(description = "Distribución de documentos activos de una categoría")
public record CategoryDistributionItemResponseDto(

        @Schema(description = "Nombre de la categoría", example = "Actas")
        String categoryName,

        @Schema(description = "Cantidad de documentos activos en la categoría", example = "18")
        long count,

        @Schema(description = "Porcentaje sobre el total de documentos activos (2 decimales)", example = "38.30")
        double percentage
) {
}
