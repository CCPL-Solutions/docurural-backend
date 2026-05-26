package co.edu.docurural.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura estándar de error usada por {@code GlobalExceptionHandler}.
 *
 * <p>Campos alineados con {@code docs/api-rest-sprint1.md} sección 1.2.
 * {@code fieldErrors} es opcional: solo se incluye en errores de validación.
 */
@Schema(description = "Estructura estándar de error de la API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponseDto(
        @Schema(description = "Momento del error en UTC", example = "2026-04-24T10:15:30")
        LocalDateTime timestamp,
        @Schema(description = "Código HTTP", example = "400")
        int status,
        @Schema(description = "Texto del estado HTTP", example = "Bad Request")
        String error,
        @Schema(description = "Mensaje descriptivo", example = "El email ya está registrado")
        String message,
        @Schema(description = "Errores por campo (solo en 400 de validación)", nullable = true)
        Map<String, String> fieldErrors
) {

    /**
     * Constructor conveniente para errores sin {@code fieldErrors}.
     */
    public static ApiErrorResponseDto of(int status, String error, String message) {
        return new ApiErrorResponseDto(LocalDateTime.now(), status, error, message, null);
    }

    /**
     * Constructor conveniente para errores de validación con mapa de campos.
     */
    public static ApiErrorResponseDto ofValidation(int status, String error, String message, Map<String, String> fieldErrors) {
        return new ApiErrorResponseDto(LocalDateTime.now(), status, error, message, fieldErrors);
    }
}
