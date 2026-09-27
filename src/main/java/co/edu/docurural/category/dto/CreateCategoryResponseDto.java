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

        @Schema(description = "Nivel de sensibilidad por defecto para documentos de esta categoría",
                example = "INTERNAL", allowableValues = {"INTERNAL", "RESTRICTED", "CONFIDENTIAL"})
        String defaultSensitivityLevel,

        @Schema(description = "Indica si los documentos de esta categoría requieren aprobación", example = "true")
        boolean requiresApproval,

        @Schema(description = "Advertencia no bloqueante cuando se activa la aprobación con menos de dos aprobadores activos",
                example = "Hay menos de dos usuarios con permiso de aprobar. Los documentos que cargue un aprobador no podrán ser aprobados por él mismo",
                nullable = true)
        String approverWarning,

        @Schema(description = "Mensaje de confirmación", example = "Categoría creada exitosamente")
        String message
) {
}
