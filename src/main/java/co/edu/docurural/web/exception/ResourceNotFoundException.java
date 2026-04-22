package co.edu.docurural.web.exception;

/**
 * Se lanza cuando una entidad referenciada por id no existe en base de datos.
 *
 * <p>El {@code GlobalExceptionHandler} (Fase 7) la mapeara a una respuesta
 * {@code 404 Not Found} con el formato estandar de error.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
