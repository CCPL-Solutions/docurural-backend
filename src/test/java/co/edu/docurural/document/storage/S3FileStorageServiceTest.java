package co.edu.docurural.document.storage;

import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.shared.exception.FileStorageException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    @Mock
    S3Client s3Client;

    @Mock
    MessageResolver messageResolver;

    S3FileStorageService s3FileStorageService;

    @BeforeEach
    void setup() {
        lenient().when(messageResolver.get(anyString())).thenAnswer(inv -> inv.getArgument(0));

        StorageProperties props = new StorageProperties();
        props.setProvider("s3");
        props.getS3().setBucket("docurural-bucket");
        props.getS3().setRegion("us-east-1");
        props.getS3().setKeyPrefix("documents");

        s3FileStorageService = new S3FileStorageService(s3Client, props, messageResolver);
    }

    @Test
    void store_uploadsObjectToS3AndReturnsRelativePath() {
        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[]{1, 2, 3});

        StoredFile stored = s3FileStorageService.store(file, DocumentFormat.PDF);

        assertThat(stored.relativePath()).endsWith(".pdf");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void load_returnsResource_whenObjectExists() throws Exception {
        ResponseBytes<GetObjectResponse> bytes = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentLength(3L).build(),
                new byte[]{9, 8, 7});
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(bytes);

        Resource resource = s3FileStorageService.load("2026/05/uuid.pdf");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isEqualTo(3L);
    }

    @Test
    void load_throwsResourceNotFound_whenS3Responds404() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        assertThatThrownBy(() -> s3FileStorageService.load("2026/05/missing.pdf"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("document.file.not-available");
    }

    @Test
    void load_throwsFileStorageException_whenPathIsInvalid() {
        assertThatThrownBy(() -> s3FileStorageService.load("../../etc/passwd"))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("document.file.not-available");
    }
}



