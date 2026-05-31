package co.edu.docurural.document.service;

import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentHashServiceTest {

    private final DocumentHashService service = new DocumentHashServiceImpl();

    @Test
    void calculateSha256_returnsHexHash_whenFileIsValid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        Optional<String> hash = service.calculateSha256(file);

        assertThat(hash).hasValue("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void calculateSha256_returnsEmpty_whenInputStreamFails() {
        MockMultipartFile file = new MockMultipartFile("file", "broken.txt", "text/plain", new byte[0]) {
            @Override
            @NonNull
            public InputStream getInputStream() throws IOException {
                throw new IOException("boom");
            }
        };

        Optional<String> hash = service.calculateSha256(file);

        assertThat(hash).isEmpty();
    }
}


