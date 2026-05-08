package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response body de {@code POST /api/documents/batch} (DOC-04 / HU-10).
 *
 * <p>Siempre retorna HTTP 200. Los fallos individuales se reportan en {@code results}.
 */
@Schema(description = "Resumen del procesamiento del lote de documentos")
public record BatchUploadDocumentResponse(

        @Schema(description = "Cantidad total de archivos recibidos en el lote", example = "3")
        int totalReceived,

        @Schema(description = "Cantidad de archivos cargados exitosamente", example = "2")
        int totalSuccessful,

        @Schema(description = "Cantidad de archivos que fallaron", example = "1")
        int totalFailed,

        @Schema(description = "Resultado individual por cada archivo del lote")
        List<BatchUploadItemResult> results
) {
}
