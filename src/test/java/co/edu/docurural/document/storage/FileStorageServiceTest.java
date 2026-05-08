package co.edu.docurural.document.storage;

import co.edu.docurural.document.enums.DocumentFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    FileStorageService fileStorageService;

    @BeforeEach
    void setup() {
        StorageProperties props = new StorageProperties();
        props.setBasePath(tempDir.toString());
        fileStorageService = new FileStorageService(props);
    }

    @Test
    void store_createsYearMonthSubdirectoryAndPersistsContent() throws IOException {
        byte[] content = "%PDF-1.4 test content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", content);

        StoredFile result = fileStorageService.store(file, DocumentFormat.PDF);

        Path storedPath = Path.of(result.absolutePath());
        assertThat(storedPath).exists();
        assertThat(Files.readAllBytes(storedPath)).isEqualTo(content);

        LocalDate now = LocalDate.now();
        String expectedYear = String.valueOf(now.getYear());
        String expectedMonth = String.format("%02d", now.getMonthValue());
        assertThat(storedPath.toString()).contains(expectedYear);
        assertThat(storedPath.toString()).contains(expectedMonth);
        assertThat(storedPath.getFileName().toString()).endsWith(".pdf");
    }

    @Test
    void store_generatesDifferentFileNamesForEachCall() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[10]);

        StoredFile result1 = fileStorageService.store(file, DocumentFormat.PDF);
        StoredFile result2 = fileStorageService.store(file, DocumentFormat.PDF);

        assertThat(result1.absolutePath()).isNotEqualTo(result2.absolutePath());
    }
}
