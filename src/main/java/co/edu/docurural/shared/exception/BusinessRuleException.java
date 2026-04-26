package co.edu.docurural.shared.exception;

/**
 * Se lanza cuando una regla de negocio del dominio se viola.
 *
 * <p>Transporta un {@link BusinessErrorCode} de dominio independiente de
 * framework. El mapeo a HTTP se realiza exclusivamente en
 * {@code GlobalExceptionHandler}.
 */
public class BusinessRuleException extends RuntimeException {

    private final BusinessErrorCode code;

    public BusinessRuleException(BusinessErrorCode code, String message) {
        super(message);
        if (code == null) {
            throw new IllegalArgumentException("code no puede ser null");
        }
        this.code = code;
    }

    public BusinessRuleException(BusinessErrorCode code, String message, Throwable cause) {
        super(message, cause);
        if (code == null) {
            throw new IllegalArgumentException("code no puede ser null");
        }
        this.code = code;
    }

    public BusinessErrorCode getCode() {
        return code;
    }
}
