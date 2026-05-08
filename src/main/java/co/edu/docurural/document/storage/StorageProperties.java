package co.edu.docurural.document.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del almacenamiento de archivos.
 *
 * <p>Propiedad {@code docurural.storage.base-path} (default {@code ./uploads/documents}).
 */
@Component
@ConfigurationProperties(prefix = "docurural.storage")
public class StorageProperties {

    private String basePath = "./uploads/documents";

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
