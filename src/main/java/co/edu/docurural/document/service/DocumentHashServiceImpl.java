package co.edu.docurural.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Implementación de cálculo de hash SHA-256 para archivos cargados.
 */
@Service
@Slf4j
public class DocumentHashServiceImpl implements DocumentHashService {

    private static final String SHA_256 = "SHA-256";

    @Override
    public Optional<String> calculateSha256(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }

        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return Optional.of(toHex(digest.digest()));
        } catch (IOException | NoSuchAlgorithmException ex) {
            log.error("No se pudo calcular hash SHA-256 para archivo '{}'", file.getOriginalFilename(), ex);
            return Optional.empty();
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

