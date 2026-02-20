package com.marcos.leairning.documents;

import com.marcos.leairning.minio.MinioDocumentStorageService;
import com.marcos.leairning.minio.MinioProcessingPipelineService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DocumentsServiceImplTest {

    DocumentsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentsServiceImpl(
                mock(DocumentsRepository.class),
                mock(DocumentsMapper.class),
                mock(MinioDocumentStorageService.class),
                mock(MinioProcessingPipelineService.class)
        );
    }

    @Test
    void validateDocument_rejectsNullFile() {
        assertThrows(IllegalArgumentException.class, () -> service.validateDocument(null));
    }

    @Test
    void validateDocument_rejectsEmptyFile() {
        val file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service.validateDocument(file));
    }

    @Test
    void validateDocument_rejectsNullContentType() {
        val file = new MockMultipartFile("file", "test.pdf", null, new byte[]{1});
        assertThrows(UnsupportedMediaTypeException.class, () -> service.validateDocument(file));
    }

    @Test
    void validateDocument_rejectsUnsupportedContentType() {
        val file = new MockMultipartFile("file", "test.exe", "application/octet-stream", new byte[]{1});
        assertThrows(UnsupportedMediaTypeException.class, () -> service.validateDocument(file));
    }

    @Test
    void validateDocument_acceptsPdf() {
        val file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{1});
        assertDoesNotThrow(() -> service.validateDocument(file));
    }

    @Test
    void validateFileContent_rejectsDisallowedContentType() {
        // A PNG image (magic bytes: 0x89 P N G)
        val pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        val file = new MockMultipartFile("file", "fake.png", "image/png", pngHeader);
        assertThrows(UnsupportedMediaTypeException.class, () -> service.validateFileContent(file));
    }

    @Test
    void validateFileContent_acceptsRealTextFile() {
        val file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello world".getBytes());
        assertDoesNotThrow(() -> service.validateFileContent(file));
    }

    @Test
    void validateFileContent_acceptsRealCsvFile() {
        val file = new MockMultipartFile("file", "data.csv", "text/csv", "name,age\nJohn,30".getBytes());
        assertDoesNotThrow(() -> service.validateFileContent(file));
    }

    @Test
    void sanitizeFilename_removesSpecialCharacters() {
        assertEquals("hello_world_test.pdf", service.sanitizeFilename("hello world!test.pdf"));
    }

    @Test
    void sanitizeFilename_rejectsNullFilename() {
        assertThrows(IllegalArgumentException.class, () -> service.sanitizeFilename(null));
    }

    @Test
    void sanitizeFilename_rejectsEmptyFilename() {
        assertThrows(IllegalArgumentException.class, () -> service.sanitizeFilename(""));
    }
}
