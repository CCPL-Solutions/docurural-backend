package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body de {@code PUT /api/documents/{id}} (DOC-05 / HU-13).
 *
 * <p>Si {@code description} se omite o llega en {@code null}, el backend mantiene
 * la descripción actual del documento.
 */
@Schema(description = "Campos editables de metadatos del documento")
public record UpdateDocumentMetadataRequest(

        @NotBlank(message = "{validation.document.title.required}")
        @Size(max = 255, message = "{validation.document.title.size}")
        @Schema(description = "Título del documento", example = "Acta Consejo Directivo Marzo 2026 - Revisado")
        String title,

        @NotNull(message = "{validation.document.category.required}")
        @Schema(description = "ID de la categoría activa", example = "1")
        Long categoryId,

        @NotBlank(message = "{validation.document.responsible-area.required}")
        @Size(max = 100, message = "{validation.document.responsible-area.size}")
        @Schema(description = "Área responsable", example = "Rectoría")
        String responsibleArea,

        @NotNull(message = "{validation.document.document-date.required}")
        @Schema(description = "Fecha del documento (YYYY-MM-DD)", example = "2026-03-15")
        LocalDate documentDate,

        @Size(max = 500, message = "{validation.document.description.size}")
        @Schema(description = "Descripción opcional. Si se omite, se conserva la actual", nullable = true,
                example = "Versión corregida del acta del 15 de marzo")
        String description
) {
}

