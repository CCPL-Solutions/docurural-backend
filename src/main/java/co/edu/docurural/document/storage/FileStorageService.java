package co.edu.docurural.document.storage;

import co.edu.docurural.document.enums.DocumentFormat;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Persiste el archivo binario en el sistema de archivos local bajo
 * {@code {basePath}/{año}/{mes}/{uuid}.{ext}}.
 *
 * <p>Los métodos devuelven / operan sobre rutas <em>relativas</em> al {@code basePath}
 * (formato {@code {year}/{month}/{uuid}.{ext}}). La ruta absoluta se resuelve
 * internamente, evitando acoplar los registros al sistema de ficheros del despliegue.
 */
public interface FileStorageService {

    StoredFile store(MultipartFile file, DocumentFormat format);

    Resource load(String relativePath);

    void delete(String relativePath);
}
