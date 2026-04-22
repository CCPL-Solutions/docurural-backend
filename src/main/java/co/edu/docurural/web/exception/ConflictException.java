package co.edu.docurural.web.exception;

/**
 * Se lanza cuando una operacion choca con una restriccion de unicidad o un
 * estado irreconciliable del recurso (p. ej. email duplicado al crear o
 * actualizar un usuario).
 *
 * <p>El {@code GlobalExceptionHandler} (Fase 7) la mapeara a una respuesta
 * {@code 409 Conflict} con el formato estandar de error.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
