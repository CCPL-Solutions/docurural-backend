package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Respuesta con el detalle completo de un documento.")
public record DocumentDetailResponseDto(
        @Schema(description = "Identificador único del documento.")
        Long id,
        @Schema(description = "Título del documento.")
        String title,
        @Schema(description = "Descripción o resumen del documento.")
        String description,
        @Schema(description = "Categoría a la que pertenece el documento.")
        CategoryRef category,
        @Schema(description = "Área responsable del documento.")
        String responsibleArea,
        @Schema(description = "Fecha asociada al documento.")
        LocalDate documentDate,
        @Schema(description = "Formato del archivo del documento.")
        String fileFormat,
        @Schema(description = "Tamaño del archivo en bytes.")
        Long fileSizeBytes,
        @Schema(description = "Nombre original del archivo cargado.")
        String originalFileName,
        @Schema(description = "Usuario que cargó el documento.")
        UploadedByRef uploadedBy,
        @Schema(description = "Fecha y hora de creación del registro.")
        LocalDateTime createdAt) {

    @Schema(description = "Referencia resumida de la categoría del documento.")
    public record CategoryRef(
            @Schema(description = "Identificador único de la categoría.")
            Long id,
            @Schema(description = "Nombre de la categoría.")
            String name
    ) {
    }

    @Schema(description = "Referencia resumida del usuario que cargó el documento.")
    public record UploadedByRef(
            @Schema(description = "Identificador único del usuario.")
            Long id,
            @Schema(description = "Nombre completo del usuario.")
            String fullName
    ) {
    }
}
