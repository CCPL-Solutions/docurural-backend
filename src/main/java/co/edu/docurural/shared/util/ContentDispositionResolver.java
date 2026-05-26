package co.edu.docurural.shared.util;

import co.edu.docurural.document.enums.DocumentFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

/**
 * Centraliza la decisión sobre si servir un archivo como {@code inline} o {@code attachment}.
 * PDF, JPG y PNG pueden abrirse en el navegador; el resto fuerza la descarga.
 */
@Component
public class ContentDispositionResolver {

    private static final EnumSet<DocumentFormat> INLINE_FORMATS =
            EnumSet.of(DocumentFormat.PDF, DocumentFormat.JPG, DocumentFormat.PNG);

    public ContentDisposition forView(DocumentFormat format, String filename) {
        if (INLINE_FORMATS.contains(format)) {
            return ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build();
        }
        return ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
    }

    public ContentDisposition forDownload(String filename) {
        return ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
    }
}
