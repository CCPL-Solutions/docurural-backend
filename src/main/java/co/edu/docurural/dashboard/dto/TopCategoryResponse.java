package co.edu.docurural.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Categoría con más documentos activos (tarjeta 3 del dashboard — HU-24).
 * Null cuando el repositorio no tiene documentos.
 */
@Schema(description = "Categoría con más documentos activos")
public record TopCategoryResponse(

        @Schema(description = "Nombre de la categoría", example = "Actas")
        String name,

        @Schema(description = "Cantidad de documentos activos en esa categoría", example = "18")
        long count
) {
}
