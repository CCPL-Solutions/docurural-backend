package co.edu.docurural.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Códigos de error de negocio que incluyen su mapeo HTTP.
 * Al añadir un código nuevo, el {@code GlobalExceptionHandler} no necesita modificarse.
 */
public enum BusinessErrorCode {
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    private final HttpStatus httpStatus;

    BusinessErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}

