package co.edu.docurural.document.storage;

import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.util.MessageResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    MessageResolver messageResolver;

    FileStorageService fileStorageService;

    @BeforeEach
    void setup() {
        lenient().when(messageResolver.get(anyString())).thenAnswer(inv -> inv.getArgument(0));
        StorageProperties props = new StorageProperties();
        props.setBasePath(tempDir.toString());
        fileStorageService = new FileStorageService(props, messageResolver);
    }

    @Test
    void store_createsYearMonthSubdirectoryAndPersistsContent() throws IOException {
        byte[] content = "%PDF-1.4 test content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", content);

        // Capturar la fecha ANTES de invocar store() para evitar flakiness en cambios de mes/año.
        LocalDate now = LocalDate.now();
        StoredFile result = fileStorageService.store(file, DocumentFormat.PDF);

        String expectedYear = String.valueOf(now.getYear());
        String expectedMonth = String.format("%02d", now.getMonthValue());

        assertThat(result.relativePath()).startsWith(expectedYear + "/" + expectedMonth + "/");
        assertThat(result.relativePath()).endsWith(".pdf");

        Path storedPath = tempDir.resolve(result.relativePath());
        assertThat(storedPath).exists();
        assertThat(Files.readAllBytes(storedPath)).isEqualTo(content);
    }

    @Test
    void store_generatesDifferentFileNamesForEachCall() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[10]);

        StoredFile result1 = fileStorageService.store(file, DocumentFormat.PDF);
        StoredFile result2 = fileStorageService.store(file, DocumentFormat.PDF);

        assertThat(result1.relativePath()).isNotEqualTo(result2.relativePath());
    }

    @Test
    void store_throwsFileStorageException_whenIOFails() {
        MockMultipartFile brokenFile = new MockMultipartFile("file", "broken.pdf", "application/pdf", new byte[0]) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("simulated disk error");
            }
        };

        assertThatThrownBy(() -> fileStorageService.store(brokenFile, DocumentFormat.PDF))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("document.file.storage-failed");
    }
}
