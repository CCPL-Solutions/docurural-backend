package co.edu.docurural.document.storage;

/**
 * Resultado de una operación de almacenamiento de archivo en disco.
 *
 * <p>{@code relativePath} es la ruta relativa al {@code basePath} configurado,
 * con formato {@code {year}/{month}/{uuid}.{ext}}. La ruta absoluta se resuelve
 * únicamente al acceder al disco, evitando acoplar los registros a la ubicación
 * del despliegue.
 */
public record StoredFile(String relativePath) {
}
