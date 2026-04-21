package co.edu.docurural.web.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando una regla de negocio del dominio se viola.
 *
 * <p>El status HTTP asociado se transporta como campo de la propia excepcion
 * para que la misma clase sirva tanto a {@code 400 Bad Request} (ej. estado
 * duplicado) como a {@code 403 Forbidden} (ej. cambio de rol propio o
 * auto-desactivacion). Esto simplifica el mapeo posterior en el
 * {@code GlobalExceptionHandler} (Fase 7).
 *
 * <p>Solo se aceptan valores 4xx distintos de 404 y 409, que ya tienen sus
 * propias excepciones ({@link ResourceNotFoundException} y {@link ConflictException}).
 */
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    public BusinessRuleException(HttpStatus status, String message) {
        super(message);
        if (status == null) {
            throw new IllegalArgumentException("status no puede ser null");
        }
        this.status = status;
    }

    public BusinessRuleException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        if (status == null) {
            throw new IllegalArgumentException("status no puede ser null");
        }
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
