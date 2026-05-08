package co.edu.docurural.document.storage;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del almacenamiento de archivos.
 *
 * <p>Propiedad {@code docurural.storage.base-path} (default {@code ./uploads/documents}).
 */
@Getter
@Component
@ConfigurationProperties(prefix = "docurural.storage")
public class StorageProperties {

    private String basePath = "./uploads/documents";

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
