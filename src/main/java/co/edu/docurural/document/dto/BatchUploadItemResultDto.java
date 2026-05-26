package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado individual de un archivo dentro de una carga en lote (DOC-04 / HU-10).
 */
@Schema(description = "Resultado de la carga de un archivo individual dentro del lote")
public record BatchUploadItemResultDto(

        @Schema(description = "Nombre original del archivo", example = "acta_enero.pdf")
        String fileName,

        @Schema(description = "true si el archivo se cargó correctamente", example = "true")
        boolean success,

        @Schema(description = "ID del documento creado. null si la carga falló", example = "48", nullable = true)
        Long documentId,

        @Schema(description = "Mensaje de error. null si la carga fue exitosa", nullable = true,
                example = "El archivo supera el tamaño máximo permitido de 10 MB")
        String errorMessage
) {
}
