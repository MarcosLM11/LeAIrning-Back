package com.marcos.documentsservice.storage;

import com.marcos.documentsservice.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileStorageService Tests")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private Long userId;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
        userId = 1L;
    }

    @Test
    @DisplayName("Should store file successfully")
    void shouldStoreFileSuccessfully() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // When
        String storagePath = fileStorageService.store(file, userId);

        // Then
        assertNotNull(storagePath);
        assertTrue(storagePath.contains(userId.toString()));
        assertTrue(Files.exists(Path.of(storagePath)));
    }

    @Test
    @DisplayName("Should generate unique filename with UUID")
    void shouldGenerateUniqueFilenameWithUUID() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // When
        String path1 = fileStorageService.store(file, userId);
        String path2 = fileStorageService.store(file, userId);

        // Then
        assertNotNull(path1);
        assertNotNull(path2);
        assertNotEquals(path1, path2);
    }

    @Test
    @DisplayName("Should organize files by user, year, and month")
    void shouldOrganizeFilesByUserYearAndMonth() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // When
        String storagePath = fileStorageService.store(file, userId);

        // Then
        assertTrue(storagePath.contains("/" + userId + "/"));
        assertTrue(storagePath.matches(".*/" + userId + "/\\d{4}/\\d{2}/.*"));
    }

    @Test
    @DisplayName("Should load file successfully")
    void shouldLoadFileSuccessfully() {
        // Given
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                content
        );
        String storagePath = fileStorageService.store(file, userId);

        // When
        byte[] loadedContent = fileStorageService.load(storagePath);

        // Then
        assertNotNull(loadedContent);
        assertArrayEquals(content, loadedContent);
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent file")
    void shouldThrowExceptionWhenLoadingNonExistentFile() {
        // Given
        String nonExistentPath = "/non/existent/path.pdf";

        // When & Then
        assertThrows(FileStorageException.class,
                () -> fileStorageService.load(nonExistentPath));
    }

    @Test
    @DisplayName("Should delete file successfully")
    void shouldDeleteFileSuccessfully() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        String storagePath = fileStorageService.store(file, userId);
        var path = Path.of(storagePath);
        assertTrue(Files.exists(path));

        // When
        fileStorageService.delete(storagePath);

        // Then
        assertFalse(Files.exists(path));
    }

    @Test
    @DisplayName("Should not throw exception when deleting non-existent file")
    void shouldNotThrowExceptionWhenDeletingNonExistentFile() {
        // Given
        String nonExistentPath = "/non/existent/path.pdf";

        // When & Then
        assertDoesNotThrow(() -> fileStorageService.delete(nonExistentPath));
    }

    @Test
    @DisplayName("Should prevent path traversal attacks")
    void shouldPreventPathTraversalAttacks() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "../../../etc/passwd",
                "text/plain",
                "malicious content".getBytes()
        );

        // When
        String storagePath = fileStorageService.store(file, userId);

        // Then
        assertNotNull(storagePath);
        assertTrue(storagePath.startsWith(tempDir.toString()));
        assertFalse(storagePath.contains("../"));
    }

    @Test
    @DisplayName("Should sanitize filename")
    void shouldSanitizeFilename() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test file with spaces & special@chars.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // When
        String storagePath = fileStorageService.store(file, userId);

        // Then
        assertNotNull(storagePath);
        String filename = Path.of(storagePath).getFileName().toString();
        assertTrue(filename.matches("[a-zA-Z0-9._-]+"));
    }

    @Test
    @DisplayName("Should throw exception when storing empty file")
    void shouldThrowExceptionWhenStoringEmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        // When & Then
        assertThrows(FileStorageException.class,
                () -> fileStorageService.store(emptyFile, userId));
    }

    @Test
    @DisplayName("Should preserve file extension")
    void shouldPreserveFileExtension() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // When
        String storagePath = fileStorageService.store(file, userId);

        // Then
        assertTrue(storagePath.endsWith(".pdf"));
    }

    @Test
    @DisplayName("Should handle files without extension")
    void shouldHandleFilesWithoutExtension() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "document",
                "application/octet-stream",
                "test content".getBytes()
        );

        // When
        String storagePath = fileStorageService.store(file, userId);

        // Then
        assertNotNull(storagePath);
        assertTrue(Files.exists(Path.of(storagePath)));
    }

    @Test
    @DisplayName("Should create directory structure if not exists")
    void shouldCreateDirectoryStructureIfNotExists() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // When
        String storagePath = fileStorageService.store(file, userId);

        // Then
        assertNotNull(storagePath);
        Path parent = Path.of(storagePath).getParent();
        assertTrue(Files.exists(parent));
        assertTrue(Files.isDirectory(parent));
    }
}