package co.edu.docurural.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Opciones disponibles para poblar los selectores del panel de filtros (SRC-02 / HU-21).
 *
 * <p>El campo {@code users} es {@code null} para roles EDITOR y READER; solo se incluye para ADMIN.
 */
@Schema(description = "Opciones de filtro para el panel de búsqueda — SRC-02")
public record FilterOptionsResponse(

        @Schema(description = "Categorías activas disponibles para el filtro")
        List<CategoryOption> categories,

        @Schema(description = "Usuarios activos disponibles para el filtro. Null para roles EDITOR y READER.", nullable = true)
        List<UserOption> users
) {

    /**
     * Opción de categoría para el selector de filtros.
     */
    @Schema(description = "Opción de categoría en el selector de filtros")
    public record CategoryOption(
            @Schema(description = "ID de la categoría", example = "1")
            Long id,
            @Schema(description = "Nombre de la categoría", example = "Actas")
            String name
    ) {
    }

    /**
     * Opción de usuario para el selector de filtros. Solo retornada para ADMIN.
     */
    @Schema(description = "Opción de usuario en el selector de filtros (solo ADMIN)")
    public record UserOption(
            @Schema(description = "ID del usuario", example = "1")
            Long id,
            @Schema(description = "Nombre completo del usuario", example = "María García López")
            String fullName
    ) {
    }
}
