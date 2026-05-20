package co.edu.docurural.shared.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Sanitiza nombres de archivo originales antes de persistirlos.
 *
 * <p>Elimina caracteres de control Unicode (incluidos RLO/LRO override que
 * invierten la apariencia del nombre), normaliza a NFC y restringe los
 * caracteres al conjunto {@code [letras, dígitos, ., _, -, espacio]}.
 */
public final class FileNameSanitizer {

    private static final Pattern UNSAFE =
            Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}._ \\-]");
    private static final int MAX_LENGTH = 200;
    private static final String FALLBACK = "archivo";

    private FileNameSanitizer() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return FALLBACK;
        }
        String normalized = Normalizer.normalize(raw.strip(), Normalizer.Form.NFKC);
        String cleaned = UNSAFE.matcher(normalized).replaceAll("_").strip();
        if (cleaned.isBlank()) {
            return FALLBACK;
        }
        return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
    }
}
