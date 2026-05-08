package co.edu.docurural.document.service;

import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Valida archivos subidos por los usuarios antes de persistirlos.
 *
 * <p>Verifica tamaño (≤ 10 MB) y tipo MIME real (magic bytes via Apache Tika).
 */
@Service
@RequiredArgsConstructor
public class FileValidationService {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private static final Map<String, DocumentFormat> ALLOWED_MIME_TYPES = Map.of(
            "application/pdf", DocumentFormat.PDF,
            "image/jpeg", DocumentFormat.JPG,
            "image/png", DocumentFormat.PNG,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", DocumentFormat.DOCX,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", DocumentFormat.XLSX
    );

    private final Tika tika = new Tika();
    private final MessageResolver messageResolver;

    /**
     * Valida el archivo y resuelve su {@link DocumentFormat}.
     *
     * @return el formato detectado
     * @throws BusinessRuleException {@code PAYLOAD_TOO_LARGE} si supera 10 MB.
     * @throws BusinessRuleException {@code UNSUPPORTED_MEDIA_TYPE} si el tipo MIME no está permitido.
     */
    public DocumentFormat validate(MultipartFile file) {
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessRuleException(BusinessErrorCode.PAYLOAD_TOO_LARGE,
                    messageResolver.get("document.file.too-large"));
        }

        String detectedMime;
        try {
            detectedMime = tika.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException ex) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messageResolver.get("document.file.read-failed"));
        }

        DocumentFormat format = ALLOWED_MIME_TYPES.get(detectedMime);
        if (format == null) {
            throw new BusinessRuleException(BusinessErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    messageResolver.get("document.file.unsupported-format"));
        }

        return format;
    }
}
