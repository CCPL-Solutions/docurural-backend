package co.edu.docurural.category.dto;

import co.edu.docurural.category.enums.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo estado de la categoría")
public record UpdateCategoryStatusRequestDto(

        @NotNull(message = "{validation.category.status.required}")
        @Schema(description = "Estado: ACTIVE | INACTIVE", example = "INACTIVE")
        CategoryStatus status
) {
}
