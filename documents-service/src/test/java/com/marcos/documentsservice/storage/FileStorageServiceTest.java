package com.marcos.documentsservice.storage;

import com.marcos.documentsservice.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileStorageService Tests")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private Path processingDir;
    private Long userId;

    @BeforeEach
    void setUp() {
        processingDir = tempDir.resolve("processing");
        fileStorageService = new FileStorageService(tempDir.toString(), processingDir.toString());
        userId = 1L;
    }

    @Test
    @DisplayName("Should store file successfully")
    void shouldStoreFileSuccessfully() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        var storagePath = fileStorageService.store(file, userId);
        assertNotNull(storagePath);
        assertTrue(storagePath.contains(userId.toString()));
        assertTrue(Files.exists(Path.of(storagePath)));
    }

    @Test
    @DisplayName("Should store bytes successfully")
    void shouldStoreBytesSuccessfully() {
        var content = "test content".getBytes();
        var storagePath = fileStorageService.store(content, "test.txt", "text/plain", userId);
        assertNotNull(storagePath);
        assertThat(storagePath).contains(userId.toString());
        assertTrue(Files.exists(Path.of(storagePath)));
    }

    @Test
    @DisplayName("Should generate unique filename with UUID")
    void shouldGenerateUniqueFilenameWithUUID() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        var path1 = fileStorageService.store(file, userId);
        var path2 = fileStorageService.store(file, userId);
        assertNotNull(path1);
        assertNotNull(path2);
        assertNotEquals(path1, path2);
    }

    @Test
    @DisplayName("Should organize files by user, year, and month")
    void shouldOrganizeFilesByUserYearAndMonth() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        var storagePath = fileStorageService.store(file, userId);
        assertTrue(storagePath.contains("/" + userId + "/") || storagePath.contains("\\" + userId + "\\"));
        assertTrue(storagePath.matches(".*[/\\\\]" + userId + "[/\\\\]\\d{4}[/\\\\]\\d{2}[/\\\\].*"));
    }

    @Test
    @DisplayName("Should load file successfully")
    void shouldLoadFileSuccessfully() {
        var content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                content
        );
        var storagePath = fileStorageService.store(file, userId);
        var loadedContent = fileStorageService.load(storagePath);
        assertNotNull(loadedContent);
        assertArrayEquals(content, loadedContent);
    }

    @Test
    @DisplayName("Should load file as stream successfully")
    void shouldLoadFileAsStreamSuccessfully() throws IOException {
        var content = "test content".getBytes();
        var storagePath = fileStorageService.store(content, "test.txt", "text/plain", userId);
        try (var stream = fileStorageService.loadAsStream(storagePath)) {
            assertNotNull(stream);
            assertArrayEquals(content, stream.readAllBytes());
        }
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent file")
    void shouldThrowExceptionWhenLoadingNonExistentFile() {
        var nonExistentPath = "/non/existent/path.pdf";
        assertThrows(FileStorageException.class,
                () -> fileStorageService.load(nonExistentPath));
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent stream")
    void shouldThrowExceptionWhenLoadingNonExistentStream() {
        var nonExistentPath = "/non/existent/path.pdf";
        assertThatThrownBy(() -> fileStorageService.loadAsStream(nonExistentPath))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Should delete file successfully")
    void shouldDeleteFileSuccessfully() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        var storagePath = fileStorageService.store(file, userId);
        var path = Path.of(storagePath);
        assertTrue(Files.exists(path));
        fileStorageService.delete(storagePath);
        assertFalse(Files.exists(path));
    }

    @Test
    @DisplayName("Should not throw exception when deleting non-existent file")
    void shouldNotThrowExceptionWhenDeletingNonExistentFile() {
        var nonExistentPath = "/non/existent/path.pdf";
        assertDoesNotThrow(() -> fileStorageService.delete(nonExistentPath));
    }

    @Test
    @DisplayName("Should prevent path traversal attacks")
    void shouldPreventPathTraversalAttacks() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "../../../etc/passwd",
                "text/plain",
                "malicious content".getBytes()
        );
        var storagePath = fileStorageService.store(file, userId);
        assertNotNull(storagePath);
        assertTrue(storagePath.startsWith(tempDir.toString()));
        assertFalse(storagePath.contains("../"));
    }

    @Test
    @DisplayName("Should sanitize filename")
    void shouldSanitizeFilename() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test file with spaces & special@chars.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        var storagePath = fileStorageService.store(file, userId);
        assertNotNull(storagePath);
        var filename = Path.of(storagePath).getFileName().toString();
        assertTrue(filename.matches("[a-zA-Z0-9._-]+"));
    }

    @Test
    @DisplayName("Should throw exception when storing empty file")
    void shouldThrowExceptionWhenStoringEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );
        assertThrows(FileStorageException.class,
                () -> fileStorageService.store(emptyFile, userId));
    }

    @Test
    @DisplayName("Should throw exception when storing empty bytes")
    void shouldThrowExceptionWhenStoringEmptyBytes() {
        assertThatThrownBy(() -> fileStorageService.store(new byte[0], "test.txt", "text/plain", userId))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Should preserve file extension")
    void shouldPreserveFileExtension() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        var storagePath = fileStorageService.store(file, userId);
        assertTrue(storagePath.endsWith(".pdf"));
    }

    @Test
    @DisplayName("Should handle files without extension")
    void shouldHandleFilesWithoutExtension() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "document",
                "application/octet-stream",
                "test content".getBytes()
        );
        var storagePath = fileStorageService.store(file, userId);
        assertNotNull(storagePath);
        assertTrue(Files.exists(Path.of(storagePath)));
    }

    @Test
    @DisplayName("Should create directory structure if not exists")
    void shouldCreateDirectoryStructureIfNotExists() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        var storagePath = fileStorageService.store(file, userId);
        assertNotNull(storagePath);
        var parent = Path.of(storagePath).getParent();
        assertTrue(Files.exists(parent));
        assertTrue(Files.isDirectory(parent));
    }

    @Test
    @DisplayName("Should copy file to processing directory")
    void shouldCopyFileToProcessingDirectory() {
        var content = "test content".getBytes();
        var storagePath = fileStorageService.store(content, "test.txt", "text/plain", userId);
        var documentId = 42L;
        var processingPath = fileStorageService.copyToProcessing(storagePath, documentId);
        assertNotNull(processingPath);
        assertThat(processingPath).contains("pending");
        assertThat(processingPath).contains("42_");
        assertTrue(Files.exists(Path.of(processingPath)));
    }

    @Test
    @DisplayName("Should throw exception when copying non-existent file to processing")
    void shouldThrowExceptionWhenCopyingNonExistentFile() {
        assertThatThrownBy(() -> fileStorageService.copyToProcessing("/non/existent/path.txt", 1L))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Should list pending files")
    void shouldListPendingFiles() {
        var content = "test content".getBytes();
        var storagePath = fileStorageService.store(content, "test.txt", "text/plain", userId);
        fileStorageService.copyToProcessing(storagePath, 1L);
        fileStorageService.copyToProcessing(storagePath, 2L);
        var pendingFiles = fileStorageService.listPendingFiles();
        assertThat(pendingFiles).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty list when no pending files")
    void shouldReturnEmptyListWhenNoPendingFiles() {
        var pendingFiles = fileStorageService.listPendingFiles();
        assertThat(pendingFiles).isEmpty();
    }

    @Test
    @DisplayName("Should mark file as processed successfully")
    void shouldMarkFileAsProcessedSuccessfully() {
        var content = "test content".getBytes();
        var storagePath = fileStorageService.store(content, "test.txt", "text/plain", userId);
        var processingPath = fileStorageService.copyToProcessing(storagePath, 42L);
        fileStorageService.markProcessed(processingPath, true);
        assertFalse(Files.exists(Path.of(processingPath)));
        var processedPath = Path.of(processingDir.toString(), "processed", "42_" + Path.of(storagePath).getFileName());
        assertTrue(Files.exists(processedPath));
    }

    @Test
    @DisplayName("Should mark file as failed")
    void shouldMarkFileAsFailed() {
        var content = "test content".getBytes();
        var storagePath = fileStorageService.store(content, "test.txt", "text/plain", userId);
        var processingPath = fileStorageService.copyToProcessing(storagePath, 42L);
        fileStorageService.markProcessed(processingPath, false);
        assertFalse(Files.exists(Path.of(processingPath)));
        var failedPath = Path.of(processingDir.toString(), "failed", "42_" + Path.of(storagePath).getFileName());
        assertTrue(Files.exists(failedPath));
    }

    @Test
    @DisplayName("Should not throw when marking non-existent processing file")
    void shouldNotThrowWhenMarkingNonExistentProcessingFile() {
        assertDoesNotThrow(() -> fileStorageService.markProcessed("/non/existent/path.txt", true));
    }

    @Test
    @DisplayName("Should load content from processing directory")
    void shouldLoadContentFromProcessingDirectory() {
        var content = "test content".getBytes();
        var storagePath = fileStorageService.store(content, "test.txt", "text/plain", userId);
        var processingPath = fileStorageService.copyToProcessing(storagePath, 42L);
        var loaded = fileStorageService.loadFromProcessing(processingPath);
        assertArrayEquals(content, loaded);
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent from processing")
    void shouldThrowExceptionWhenLoadingNonExistentFromProcessing() {
        assertThatThrownBy(() -> fileStorageService.loadFromProcessing("/non/existent/path.txt"))
                .isInstanceOf(StorageException.class);
    }
}