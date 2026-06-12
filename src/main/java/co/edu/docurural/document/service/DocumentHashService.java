package co.edu.docurural.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Calcula códigos de integridad para archivos cargados al módulo de documentos.
 */
public interface DocumentHashService {

    /**
     * Calcula el hash SHA-256 del contenido binario del archivo recibido.
     *
     * @return hash en hexadecimal (64 chars) o vacío si no pudo calcularse.
     */
    Optional<String> calculateSha256(MultipartFile file);
}

