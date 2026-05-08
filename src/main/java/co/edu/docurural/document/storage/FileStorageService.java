package co.edu.docurural.document.storage;

import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Persiste el archivo binario en el sistema de archivos local bajo
 * {@code {basePath}/{año}/{mes}/{uuid}.{ext}}.
 *
 * <p>Los métodos devuelven / operan sobre rutas <em>relativas</em> al {@code basePath}
 * (formato {@code {year}/{month}/{uuid}.{ext}}). La ruta absoluta se resuelve
 * internamente, evitando acoplar los registros al sistema de ficheros del despliegue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final StorageProperties storageProperties;
    private final MessageResolver messageResolver;

    public StoredFile store(MultipartFile file, DocumentFormat format) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String extension = format.name().toLowerCase();
        String fileName = UUID.randomUUID() + "." + extension;

        // La ruta relativa usa "/" explícitamente (no File.separator) para que sea
        // portátil como clave de almacenamiento independiente del SO del despliegue.
        String relativePath = String.join("/", year, month, fileName);
        Path directory = Paths.get(storageProperties.getBasePath(), year, month);

        try {
            Files.createDirectories(directory);
            Path destination = directory.resolve(fileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Archivo almacenado en: {} (relativo: {})", destination.toAbsolutePath(), relativePath);
            return new StoredFile(relativePath);
        } catch (IOException ex) {
            log.error("Error al almacenar archivo: {}", ex.getMessage(), ex);
            throw new FileStorageException(messageResolver.get("document.file.storage-failed"), ex);
        }
    }

    /**
     * Elimina el archivo identificado por su ruta relativa al {@code basePath}.
     * Se usa como operación compensatoria cuando la transacción es revertida.
     * Los errores se logean pero no se propagan para no ocultar la excepción original.
     *
     * @param relativePath ruta relativa devuelta por {@link #store}
     */
    public void delete(String relativePath) {
        try {
            Path fullPath = Paths.get(storageProperties.getBasePath()).resolve(relativePath);
            boolean deleted = Files.deleteIfExists(fullPath);
            if (deleted) {
                log.warn("Archivo huérfano eliminado tras rollback: {}", fullPath);
            }
        } catch (IOException ex) {
            log.error("No se pudo eliminar archivo huérfano '{}': {}", relativePath, ex.getMessage(), ex);
        }
    }
}
