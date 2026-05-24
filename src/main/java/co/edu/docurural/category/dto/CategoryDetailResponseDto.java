package co.edu.docurural.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Detalle de una categoría documental")
public record CategoryDetailResponseDto(
        @Schema(description = "ID de la categoría", example = "1") Long id,
        @Schema(description = "Nombre", example = "Actas") String name,
        @Schema(description = "Descripción", example = "Actas de reuniones, consejos directivos, comités")
        String description,
        @Schema(description = "Estado", example = "ACTIVE") String status,
        @Schema(description = "Cantidad de documentos ACTIVE asociados", example = "23") Long documentCount,
        @Schema(description = "Fecha de creación") LocalDateTime createdAt,
        @Schema(description = "Nombre completo del creador (\"Sistema\" si fue sembrada)", example = "Carlos Ramírez Pinzón")
        String createdBy
) {
}
