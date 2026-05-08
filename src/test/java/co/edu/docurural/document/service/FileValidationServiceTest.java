package co.edu.docurural.document.service;

import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.storage.StorageProperties;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.util.MessageResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FileValidationServiceTest {

    // PDF magic bytes: %PDF-
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D};
    // PNG magic bytes: \x89PNG\r\n\x1a\n
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    // JPEG magic bytes: \xFF\xD8\xFF
    private static final byte[] JPG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    @Mock
    MessageResolver messageResolver;

    FileValidationService fileValidationService;

    @BeforeEach
    void setup() {
        lenient().when(messageResolver.get(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageResolver.get(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
        StorageProperties props = new StorageProperties();
        props.setMaxFileSize(DataSize.ofMegabytes(10));
        fileValidationService = new FileValidationService(props, messageResolver);
    }

    @Test
    void validate_returnsPdfFormat_forValidPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", PDF_MAGIC);

        DocumentFormat result = fileValidationService.validate(file);

        assertThat(result).isEqualTo(DocumentFormat.PDF);
    }

    @Test
    void validate_returnsPngFormat_forValidPng() {
        MockMultipartFile file = new MockMultipartFile("file", "img.png", "image/png", PNG_MAGIC);

        DocumentFormat result = fileValidationService.validate(file);

        assertThat(result).isEqualTo(DocumentFormat.PNG);
    }

    @Test
    void validate_returnsJpgFormat_forValidJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "img.jpg", "image/jpeg", JPG_MAGIC);

        DocumentFormat result = fileValidationService.validate(file);

        assertThat(result).isEqualTo(DocumentFormat.JPG);
    }

    @Test
    void validate_throwsUnsupportedMediaType_forTextFile() {
        byte[] textContent = "This is plain text content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "hack.pdf", "text/plain", textContent);

        assertThatThrownBy(() -> fileValidationService.validate(file))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void validate_throwsPayloadTooLarge_whenExceedsMaxSize() {
        // 10 MB + 1 byte
        byte[] bigContent = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", bigContent);

        assertThatThrownBy(() -> fileValidationService.validate(file))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.PAYLOAD_TOO_LARGE);
    }

    @Test
    void validate_respectsConfiguredLimit_whenLimitIsSmall() {
        StorageProperties smallLimitProps = new StorageProperties();
        smallLimitProps.setMaxFileSize(DataSize.ofBytes(10));
        FileValidationService smallLimitService = new FileValidationService(smallLimitProps, messageResolver);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[11]);

        assertThatThrownBy(() -> smallLimitService.validate(file))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo(BusinessErrorCode.PAYLOAD_TOO_LARGE);
    }
}
