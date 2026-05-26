package co.edu.docurural.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body de {@code POST /api/categories} (CAT-03 / HU-16).
 */
@Schema(description = "Datos para crear una nueva categoría documental")
public record CreateCategoryRequestDto(

        @NotBlank(message = "{validation.category.name.required}")
        @Size(min = 3, max = 100, message = "{validation.category.name.size}")
        @Schema(description = "Nombre único de la categoría", example = "Proyectos Biotecnología")
        String name,

        @Size(max = 500, message = "{validation.category.description.size}")
        @Schema(description = "Descripción opcional del tipo de documentos que agrupa",
                example = "Proyectos e informes del laboratorio de biotecnología en tejido vegetal")
        String description
) {
}
