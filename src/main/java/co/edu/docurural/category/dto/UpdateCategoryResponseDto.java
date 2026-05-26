package co.edu.docurural.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body de {@code PUT /api/categories/{id}} (CAT-04 / HU-17).
 */
@Schema(description = "Categoría actualizada")
public record UpdateCategoryResponseDto(
        @Schema(description = "Identificador único de la categoría", example = "9")
        Long id,
        @Schema(description = "Nombre actualizado de la categoría", example = "Proyectos e Informes Biotecnología")
        String name,
        @Schema(description = "Descripción actualizada", example = "Proyectos e informes detallados del programa de Biotecnología", nullable = true)
        String description,
        @Schema(description = "Estado actual de la categoría", example = "ACTIVE")
        String status,
        @Schema(description = "Mensaje de confirmación", example = "Categoría actualizada exitosamente")
        String message
) {
}
