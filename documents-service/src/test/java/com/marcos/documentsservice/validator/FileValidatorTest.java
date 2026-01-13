package com.marcos.documentsservice.validator;

import com.marcos.documentsservice.exception.InvalidFileTypeException;
import com.marcos.documentsservice.exception.UnsupportedMediaTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileValidator Tests")
class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator();
    }

    @Test
    @DisplayName("Should validate PDF file successfully")
    void shouldValidatePdfFileSuccessfully() {
        // Given
        byte[] pdfContent = createPdfMagicBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                pdfContent
        );

        // When & Then
        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should validate TXT file successfully")
    void shouldValidateTxtFileSuccessfully() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "Plain text content".getBytes()
        );

        // When & Then
        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should validate CSV file successfully")
    void shouldValidateCsvFileSuccessfully() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "data.csv",
                "text/csv",
                "col1,col2,col3\nval1,val2,val3".getBytes()
        );

        // When & Then
        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should validate DOC file successfully")
    void shouldValidateDocFileSuccessfully() {
        // Given
        byte[] docContent = createDocMagicBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.doc",
                "application/msword",
                docContent
        );

        // When & Then
        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should validate DOCX file successfully")
    void shouldValidateDocxFileSuccessfully() {
        // Given
        byte[] docxContent = createDocxMagicBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxContent
        );

        // When & Then
        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should validate Markdown file successfully")
    void shouldValidateMarkdownFileSuccessfully() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "README.md",
                "text/markdown",
                "# Markdown Content\n## Subtitle".getBytes()
        );

        // When & Then
        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should throw exception for unsupported MIME type")
    void shouldThrowExceptionForUnsupportedMimeType() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        // When & Then
        assertThrows(UnsupportedMediaTypeException.class,
                () -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should throw exception when MIME type does not match file content")
    void shouldThrowExceptionWhenMimeTypeDoesNotMatchFileContent() {
        // Given - File claims to be PDF but content is plain text
        MultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "This is not a PDF file".getBytes()
        );

        // When & Then
        assertThrows(InvalidFileTypeException.class,
                () -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should throw exception for executable files")
    void shouldThrowExceptionForExecutableFiles() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/x-msdownload",
                "MZ".getBytes()
        );

        // When & Then
        assertThrows(UnsupportedMediaTypeException.class,
                () -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should throw exception for script files")
    void shouldThrowExceptionForScriptFiles() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "script.js",
                "application/javascript",
                "alert('malicious')".getBytes()
        );

        // When & Then
        assertThrows(UnsupportedMediaTypeException.class,
                () -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should sanitize filename correctly")
    void shouldSanitizeFilenameCorrectly() {
        // When
        String sanitized = fileValidator.sanitizeFilename("test file@#$%.pdf");

        // Then
        assertEquals("test_file____.pdf", sanitized);
        assertTrue(sanitized.matches("[a-zA-Z0-9._-]+"));
    }

    @Test
    @DisplayName("Should preserve valid characters in filename")
    void shouldPreserveValidCharactersInFilename() {
        // When
        String sanitized = fileValidator.sanitizeFilename("valid-file_name.123.pdf");

        // Then
        assertEquals("valid-file_name.123.pdf", sanitized);
    }

    @Test
    @DisplayName("Should handle null or empty filename")
    void shouldHandleNullOrEmptyFilename() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> fileValidator.sanitizeFilename(null));
        assertThrows(IllegalArgumentException.class,
                () -> fileValidator.sanitizeFilename(""));
    }

    @Test
    @DisplayName("Should warn when file size exceeds soft limit")
    void shouldWarnWhenFileSizeExceedsSoftLimit() {
        // Given - 6GB file (exceeds 5GB soft limit)
        byte[] pdfContent = createPdfMagicBytes();
        MultipartFile largeMockFile = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                pdfContent
        ) {
            @Override
            public long getSize() {
                return 6L * 1024 * 1024 * 1024; // 6GB
            }
        };

        // When & Then - Should not throw exception, just log warning
        assertDoesNotThrow(() -> fileValidator.validate(largeMockFile));
    }

    @Test
    @DisplayName("Should accept file within soft limit")
    void shouldAcceptFileWithinSoftLimit() {
        // Given - 1GB file (within 5GB soft limit)
        byte[] pdfContent = createPdfMagicBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "normal.pdf",
                "application/pdf",
                pdfContent
        ) {
            @Override
            public long getSize() {
                return (long) 1024 * 1024 * 1024; // 1GB
            }
        };

        // When & Then
        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    @DisplayName("Should throw exception for null file")
    void shouldThrowExceptionForNullFile() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> fileValidator.validate(null));
    }

    @Test
    @DisplayName("Should throw exception for file with null content type")
    void shouldThrowExceptionForFileWithNullContentType() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                null,
                "content".getBytes()
        );

        // When & Then
        assertThrows(UnsupportedMediaTypeException.class,
                () -> fileValidator.validate(file));
    }

    // Helper methods to create magic bytes for different file types
    private byte[] createPdfMagicBytes() {
        return "%PDF-1.4\n".getBytes();
    }

    private byte[] createDocMagicBytes() {
        // DOC files start with D0 CF 11 E0 A1 B1 1A E1
        return new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    }

    private byte[] createDocxMagicBytes() {
        // DOCX files are ZIP archives starting with PK
        return new byte[]{'P', 'K', 0x03, 0x04};
    }
}