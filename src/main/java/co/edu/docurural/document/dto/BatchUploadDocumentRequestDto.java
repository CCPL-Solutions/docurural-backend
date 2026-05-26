package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Campos de texto del request multipart {@code POST /api/documents/batch} (DOC-04 / HU-10).
 *
 * <p>El array de archivos {@code files} se recibe como {@code @RequestPart} separado en el controller.
 * Los metadatos comunes aplican a todos los archivos del lote.
 */
@Schema(description = "Metadatos comunes para la carga de un lote de documentos")
public record BatchUploadDocumentRequestDto(

        @NotNull(message = "{validation.document.category.required}")
        @Schema(description = "ID de la categoría activa aplicada a todos los archivos del lote", example = "1")
        Long categoryId,

        @NotBlank(message = "{validation.document.responsible-area.required}")
        @Size(max = 100, message = "{validation.document.responsible-area.size}")
        @Schema(description = "Área responsable aplicada a todos los archivos del lote", example = "Rectoría")
        String responsibleArea,

        @Valid
        @Schema(description = "Títulos por archivo (en el mismo orden que files[]). "
                + "Si se omite o la posición está vacía, se usa el nombre original del archivo. "
                + "Cada título debe tener entre 2 y 255 caracteres.",
                nullable = true,
                example = "[\"Acta Enero 2026\", \"Acta Febrero 2026\"]")
        List<@NotBlank(message = "{validation.document.batch.title.blank}")
             @Size(min = 2, max = 255, message = "{validation.document.batch.title.size}") String> titles
) {
}
