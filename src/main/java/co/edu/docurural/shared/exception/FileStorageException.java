package co.edu.docurural.shared.exception;

/**
 * Se lanza cuando el sistema de archivos no puede persistir un archivo cargado.
 *
 * <p>{@link GlobalExceptionHandler} la mapea a HTTP 500 con el mensaje contractual
 * {@code document.file.storage-failed}, sin filtrar detalles internos al cliente.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
