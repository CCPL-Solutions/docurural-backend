package co.edu.docurural.document.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * Configuración del almacenamiento de archivos.
 *
 * <ul>
 *   <li>{@code docurural.storage.base-path} — directorio base (default {@code ./uploads/documents}).</li>
 *   <li>{@code docurural.storage.max-file-size} — límite por archivo (default {@code 10MB}).
 *       {@code spring.servlet.multipart.max-file-size} referencia este mismo valor.</li>
 * </ul>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "docurural.storage")
public class StorageProperties {

    private String basePath = "./uploads/documents";

    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

}
