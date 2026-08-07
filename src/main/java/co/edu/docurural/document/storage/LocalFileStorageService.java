package co.edu.docurural.document.storage;

import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Implementación de almacenamiento local en sistema de archivos.
 */
@Service
@ConditionalOnProperty(prefix = "docurural.storage", name = "provider", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties storageProperties;
    private final MessageResolver messageResolver;

    @Override
    public StoredFile store(MultipartFile file, DocumentFormat format) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String extension = format.name().toLowerCase();
        String fileName = UUID.randomUUID() + "." + extension;

        String relativePath = String.join("/", year, month, fileName);
        Path directory = Paths.get(storageProperties.getBasePath(), year, month);

        try {
            Files.createDirectories(directory);
            Path destination = directory.resolve(fileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Archivo almacenado en disco: {} (relativo: {})", destination.toAbsolutePath(), relativePath);
            return new StoredFile(relativePath);
        } catch (IOException ex) {
            log.error("Error al almacenar archivo local: {}", ex.getMessage(), ex);
            throw new FileStorageException(messageResolver.get("document.file.storage-failed"), ex);
        }
    }

    @Override
    public Resource load(String relativePath) {
        Path basePath = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
        Path absolute = basePath.resolve(relativePath).normalize();

        if (!absolute.startsWith(basePath)) {
            log.warn("Intento de path traversal detectado: '{}'", relativePath);
            throw new FileStorageException(messageResolver.get("document.file.not-available"));
        }
        if (!Files.exists(absolute) || !Files.isRegularFile(absolute)) {
            throw new ResourceNotFoundException(messageResolver.get("document.file.not-available"));
        }
        try {
            return new UrlResource(absolute.toUri());
        } catch (MalformedURLException ex) {
            throw new FileStorageException(messageResolver.get("document.file.load-failed"), ex);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path basePath = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
            Path fullPath = basePath.resolve(relativePath).normalize();
            if (!fullPath.startsWith(basePath)) {
                log.warn("Intento de path traversal en delete detectado: '{}'", relativePath);
                return;
            }
            boolean deleted = Files.deleteIfExists(fullPath);
            if (deleted) {
                log.warn("Archivo huérfano eliminado tras rollback: {}", fullPath);
            }
        } catch (IOException ex) {
            log.error("No se pudo eliminar archivo huérfano '{}': {}", relativePath, ex.getMessage(), ex);
        }
    }
}

