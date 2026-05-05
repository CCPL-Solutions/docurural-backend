package co.edu.docurural.shared.exception;

/**
 * Se lanza cuando una operación choca con una restricción de unicidad o un
 * estado irreconciliable del recurso (p. ej. email duplicado al crear o
 * actualizar un usuario).
 *
 * <p>El {@code GlobalExceptionHandler} (Fase 7) la mapeará a una respuesta
 * {@code 409 Conflict} con el formato estándar de error.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
