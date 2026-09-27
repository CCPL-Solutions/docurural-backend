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
        @Schema(description = "Nivel de sensibilidad por defecto para documentos de esta categoría",
                example = "INTERNAL", allowableValues = {"INTERNAL", "RESTRICTED", "CONFIDENTIAL"})
        String defaultSensitivityLevel,
        @Schema(description = "Indica si los documentos de esta categoría requieren aprobación", example = "true")
        boolean requiresApproval,
        @Schema(description = "Aviso de alcance cuando cambia el valor de requiresApproval",
                example = "Este cambio solo afecta a los documentos que se carguen desde ahora. Los documentos existentes conservan su estado actual",
                nullable = true)
        String approvalScopeNotice,
        @Schema(description = "Advertencia no bloqueante cuando se activa la aprobación con menos de dos aprobadores activos",
                example = "Hay menos de dos usuarios con permiso de aprobar. Los documentos que cargue un aprobador no podrán ser aprobados por él mismo",
                nullable = true)
        String approverWarning,
        @Schema(description = "Mensaje de confirmación", example = "Categoría actualizada exitosamente")
        String message
) {
}
