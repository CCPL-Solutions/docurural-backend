package co.edu.docurural.category.dto;

import co.edu.docurural.shared.enums.SensitivityLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body de {@code PUT /api/categories/{id}} (CAT-04 / HU-17 / HU-28B).
 */
@Schema(description = "Campos editables de una categoría documental")
public record UpdateCategoryRequestDto(

        @NotBlank(message = "{validation.category.name.required}")
        @Size(min = 3, max = 100, message = "{validation.category.name.size}")
        @Schema(description = "Nuevo nombre único de la categoría", example = "Proyectos e Informes Biotecnología")
        String name,

        @Size(max = 500, message = "{validation.category.description.size}")
        @Schema(description = "Nueva descripción (null para conservar la actual)",
                example = "Proyectos e informes detallados del programa de Biotecnología en Tejido Vegetal")
        String description,

        @NotNull(message = "{validation.category.default-sensitivity.required}")
        @Schema(description = "Nivel de sensibilidad que heredan los documentos de esta categoría por defecto",
                example = "INTERNAL", allowableValues = {"INTERNAL", "RESTRICTED", "CONFIDENTIAL"})
        SensitivityLevel defaultSensitivityLevel
) {
}
