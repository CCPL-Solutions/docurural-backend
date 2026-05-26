package co.edu.docurural.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response body de {@code POST /api/categories} (CAT-03 / HU-16).
 */
@Schema(description = "Categoría documental recién creada")
public record CreateCategoryResponseDto(

        @Schema(description = "Identificador único de la categoría", example = "9")
        Long id,

        @Schema(description = "Nombre de la categoría", example = "Proyectos Biotecnología")
        String name,

        @Schema(description = "Descripción de la categoría (null si no fue especificada)",
                example = "Proyectos e informes del laboratorio de biotecnología en tejido vegetal",
                nullable = true)
        String description,

        @Schema(description = "Estado de la categoría — siempre ACTIVE al crearse", example = "ACTIVE")
        String status,

        @Schema(description = "Fecha y hora de creación", example = "2026-04-17T10:15:00")
        LocalDateTime createdAt,

        @Schema(description = "Mensaje de confirmación", example = "Categoría creada exitosamente")
        String message
) {
}
