package co.edu.docurural.document.storage;

import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.ResponseBytes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Implementación de almacenamiento sobre AWS S3.
 */
@Service
@ConditionalOnProperty(prefix = "docurural.storage", name = "provider", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;
    private final MessageResolver messageResolver;

    @Override
    public StoredFile store(MultipartFile file, DocumentFormat format) {
        String relativePath = buildRelativePath(format);
        String key = buildObjectKey(relativePath);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(key)
                    .contentType(format.toMediaType().toString())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.debug("Archivo almacenado en S3: bucket={} key={} relativePath={}", requireBucket(), key, relativePath);
            return new StoredFile(relativePath);
        } catch (IOException | S3Exception | SdkClientException ex) {
            log.error("Error al almacenar archivo en S3: {}", ex.getMessage(), ex);
            throw new FileStorageException(messageResolver.get("document.file.storage-failed"), ex);
        }
    }

    @Override
    public Resource load(String relativePath) {
        String key;
        try {
            String safeRelativePath = validateRelativePath(relativePath);
            key = buildObjectKey(safeRelativePath);
        } catch (IllegalArgumentException ex) {
            throw new FileStorageException(messageResolver.get("document.file.not-available"));
        }

        try {
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(key)
                    .build());
            return new ByteArrayResource(bytes.asByteArray());
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ResourceNotFoundException(messageResolver.get("document.file.not-available"));
            }
            throw new FileStorageException(messageResolver.get("document.file.load-failed"), ex);
        } catch (SdkClientException ex) {
            throw new FileStorageException(messageResolver.get("document.file.load-failed"), ex);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            String safeRelativePath = validateRelativePath(relativePath);
            String key = buildObjectKey(safeRelativePath);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(key)
                    .build());
            log.warn("Archivo huérfano eliminado en S3 tras rollback: bucket={} key={}", requireBucket(), key);
        } catch (IllegalArgumentException ex) {
            log.warn("Ruta inválida para eliminación en S3: '{}': {}", relativePath, ex.getMessage());
        } catch (S3Exception | SdkClientException ex) {
            log.error("No se pudo eliminar archivo huérfano en S3 '{}': {}", relativePath, ex.getMessage(), ex);
        }
    }

    private String buildRelativePath(DocumentFormat format) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String extension = format.name().toLowerCase();
        return String.join("/", year, month, UUID.randomUUID() + "." + extension);
    }

    private String buildObjectKey(String relativePath) {
        String keyPrefix = storageProperties.getS3().getKeyPrefix();
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return relativePath;
        }
        String normalizedPrefix = keyPrefix.replace("\\", "/").replaceAll("^/+|/+$", "");
        return normalizedPrefix + "/" + relativePath;
    }

    private String requireBucket() {
        String bucket = storageProperties.getS3().getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("La propiedad docurural.storage.s3.bucket es obligatoria cuando provider=s3");
        }
        return bucket;
    }

    private String validateRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath no puede ser vacío");
        }
        String normalized = relativePath.replace("\\", "/");
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.contains("..\\")
                || normalized.contains("//") || normalized.equals("..")) {
            throw new IllegalArgumentException("relativePath inválido");
        }
        return normalized;
    }
}


