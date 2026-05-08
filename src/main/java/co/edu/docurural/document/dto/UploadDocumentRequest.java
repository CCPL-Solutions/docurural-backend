package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Campos de texto del request multipart {@code POST /api/documents} (DOC-03 / HU-09).
 *
 * <p>El archivo {@code file} se recibe como {@code @RequestPart} separado en el controller.
 */
@Schema(description = "Metadatos del documento a cargar")
public record UploadDocumentRequest(

        @NotBlank(message = "{validation.document.title.required}")
        @Size(max = 255, message = "{validation.document.title.size}")
        @Schema(description = "Título descriptivo del documento", example = "Acta Consejo Directivo Marzo 2026")
        String title,

        @NotNull(message = "{validation.document.category.required}")
        @Schema(description = "ID de la categoría activa del documento", example = "1")
        Long categoryId,

        @NotBlank(message = "{validation.document.responsible-area.required}")
        @Size(max = 100, message = "{validation.document.responsible-area.size}")
        @Schema(description = "Área de la institución responsable del documento", example = "Rectoría")
        String responsibleArea,

        @NotNull(message = "{validation.document.document-date.required}")
        @Schema(description = "Fecha del documento (no la de carga)", example = "2026-03-15")
        LocalDate documentDate,

        @Size(max = 500, message = "{validation.document.description.size}")
        @Schema(description = "Descripción opcional del contenido",
                example = "Acta de la reunión del consejo directivo del 15 de marzo de 2026",
                nullable = true)
        String description
) {
}
