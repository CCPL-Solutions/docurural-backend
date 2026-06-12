package co.edu.docurural.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado del cambio de estado de categoría")
public record UpdateCategoryStatusResponseDto(

        @Schema(description = "ID de la categoría", example = "9")
        Long id,

        @Schema(description = "Nombre de la categoría", example = "Proyectos e Informes Biotecnología")
        String name,

        @Schema(description = "Estado aplicado", example = "INACTIVE")
        String status,

        @Schema(description = "Mensaje de resultado", example = "Categoría desactivada exitosamente")
        String message
) {
}
