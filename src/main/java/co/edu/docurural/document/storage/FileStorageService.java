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

        Path directory = Paths.get(storageProperties.getBasePath(), year, month);

        try {
            Files.createDirectories(directory);
            Path destination = directory.resolve(fileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Archivo almacenado en: {}", destination.toAbsolutePath());
            return new StoredFile(destination.toAbsolutePath().toString());
        } catch (IOException ex) {
            log.error("Error al almacenar archivo: {}", ex.getMessage(), ex);
            throw new FileStorageException(messageResolver.get("document.file.storage-failed"), ex);
        }
    }
}
