package co.edu.docurural.document.storage;

/**
 * Resultado de una operación de almacenamiento de archivo.
 *
 * <p>{@code relativePath} mantiene el formato {@code {year}/{month}/{uuid}.{ext}}
 * y se persiste desacoplado del backend físico (disco local o S3).
 */
public record StoredFile(String relativePath) {
}
