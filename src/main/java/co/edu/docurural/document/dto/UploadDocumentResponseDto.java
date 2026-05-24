package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response body de {@code POST /api/documents} (DOC-03 / HU-09).
 */
@Schema(description = "Documento recién cargado al sistema")
public record UploadDocumentResponseDto(

        @Schema(description = "Identificador único del documento creado", example = "48")
        Long id,

        @Schema(description = "Título del documento", example = "Acta Consejo Directivo Marzo 2026")
        String title,

        @Schema(description = "Nombre de la categoría asignada", example = "Actas")
        String category,

        @Schema(description = "Área responsable del documento", example = "Rectoría")
        String responsibleArea,

        @Schema(description = "Fecha del documento (YYYY-MM-DD)", example = "2026-03-15")
        LocalDate documentDate,

        @Schema(description = "Formato del archivo cargado", example = "PDF")
        String fileFormat,

        @Schema(description = "Tamaño del archivo en bytes", example = "524288")
        Long fileSizeBytes,

        @Schema(description = "Nombre original del archivo al subirse", example = "acta_consejo_directivo.pdf")
        String originalFileName,

        @Schema(description = "Fecha y hora de carga al sistema", example = "2026-04-17T10:20:00")
        LocalDateTime createdAt,

        @Schema(description = "Mensaje de confirmación", example = "Documento cargado exitosamente")
        String message
) {
}
