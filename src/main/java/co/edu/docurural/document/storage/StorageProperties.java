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

    /**
     * Proveedor de almacenamiento: local | s3.
     */
    private String provider = "local";

    private String basePath = "./uploads/documents";

    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    private S3Properties s3 = new S3Properties();

    @Setter
    @Getter
    public static class S3Properties {
        private String bucket;
        private String region = "us-east-1";
        private String keyPrefix = "documents";
    }

}
