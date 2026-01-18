package com.marcos.documentsservice.storage;

import io.minio.*;
import io.minio.messages.DeleteObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.LinkedList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("MinioStorageService Tests")
class MinioStorageServiceTest {

    private static final String DOCUMENTS_BUCKET = "test-documents";
    private static final String PROCESSING_BUCKET = "test-processing";

    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:RELEASE.2024-12-18T13-15-44Z")
            .withUserName("testuser")
            .withPassword("testpassword");

    private MinioStorageService storageService;
    private MinioClient minioClient;

    @BeforeEach
    void setUp() throws Exception {
        minioClient = MinioClient.builder()
                .endpoint(minioContainer.getS3URL())
                .credentials(minioContainer.getUserName(), minioContainer.getPassword())
                .build();
        createBucketIfNotExists(DOCUMENTS_BUCKET);
        createBucketIfNotExists(PROCESSING_BUCKET);
        clearBucket(DOCUMENTS_BUCKET);
        clearBucket(PROCESSING_BUCKET);
        storageService = new MinioStorageService(minioClient, DOCUMENTS_BUCKET, PROCESSING_BUCKET);
    }

    private void createBucketIfNotExists(String bucketName) throws Exception {
        var exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private void clearBucket(String bucketName) throws Exception {
        var objects = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).recursive(true).build());
        var objectsToDelete = new LinkedList<DeleteObject>();
        for (var result : objects) {
            objectsToDelete.add(new DeleteObject(result.get().objectName()));
        }
        if (!objectsToDelete.isEmpty()) {
            var results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(objectsToDelete)
                    .build());
            for (var result : results) {
                result.get();
            }
        }
    }

    @Test
    @DisplayName("Should store content and return path")
    void store_shouldUploadAndReturnPath() {
        var content = "test content".getBytes();
        var filename = "test.txt";
        var contentType = "text/plain";
        var userId = 123L;
        var storagePath = storageService.store(content, filename, contentType, userId);
        assertThat(storagePath).matches("123/\\d{4}/\\d{2}/[a-f0-9-]+\\.txt");
    }

    @Test
    @DisplayName("Should load stored content")
    void load_shouldReturnStoredContent() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test.txt", "text/plain", 1L);
        var loaded = storageService.load(storagePath);
        assertThat(loaded).isEqualTo(content);
    }

    @Test
    @DisplayName("Should load content as stream")
    void loadAsStream_shouldReturnInputStream() throws Exception {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test.txt", "text/plain", 1L);
        try (var stream = storageService.loadAsStream(storagePath)) {
            assertThat(stream.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    @DisplayName("Should delete stored object")
    void delete_shouldRemoveObject() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test.txt", "text/plain", 1L);
        storageService.delete(storagePath);
        assertThatThrownBy(() -> storageService.load(storagePath))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent object")
    void load_shouldThrowWhenNotFound() {
        assertThatThrownBy(() -> storageService.load("non/existent/path.txt"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Should throw exception when storing empty content")
    void store_shouldThrowWhenEmpty() {
        assertThatThrownBy(() -> storageService.store(new byte[0], "test.txt", "text/plain", 1L))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Should copy to processing bucket")
    void copyToProcessing_shouldCopyToProcessingBucket() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test.txt", "text/plain", 1L);
        var documentId = 42L;
        var processingPath = storageService.copyToProcessing(storagePath, documentId);
        assertThat(processingPath).startsWith("pending/42_");
        var pendingFiles = storageService.listPendingFiles();
        assertThat(pendingFiles).contains(processingPath);
    }

    @Test
    @DisplayName("Should list pending files")
    void listPendingFiles_shouldReturnPendingFiles() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test.txt", "text/plain", 1L);
        storageService.copyToProcessing(storagePath, 1L);
        storageService.copyToProcessing(storagePath, 2L);
        var pendingFiles = storageService.listPendingFiles();
        assertThat(pendingFiles).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty list when no pending files")
    void listPendingFiles_shouldReturnEmptyWhenNoPending() {
        var pendingFiles = storageService.listPendingFiles();
        assertThat(pendingFiles).isEmpty();
    }

    @Test
    @DisplayName("Should mark file as processed")
    void markProcessed_shouldMoveToProcessedFolder() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test.txt", "text/plain", 1L);
        var processingPath = storageService.copyToProcessing(storagePath, 42L);
        storageService.markProcessed(processingPath, true);
        var pendingFiles = storageService.listPendingFiles();
        assertThat(pendingFiles).doesNotContain(processingPath);
    }

    @Test
    @DisplayName("Should mark file as failed")
    void markProcessed_shouldMoveToFailedFolder() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test.txt", "text/plain", 1L);
        var processingPath = storageService.copyToProcessing(storagePath, 42L);
        storageService.markProcessed(processingPath, false);
        var pendingFiles = storageService.listPendingFiles();
        assertThat(pendingFiles).doesNotContain(processingPath);
    }

    @Test
    @DisplayName("Should sanitize filename with special characters")
    void store_shouldSanitizeFilename() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test file@special#chars.txt", "text/plain", 1L);
        assertThat(storagePath).matches("1/\\d{4}/\\d{2}/[a-f0-9-]+\\.txt");
    }

    @Test
    @DisplayName("Should handle null filename")
    void store_shouldHandleNullFilename() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, null, "text/plain", 1L);
        assertThat(storagePath).isNotNull();
        assertThat(storagePath).matches("1/\\d{4}/\\d{2}/[a-f0-9-]+");
    }

    @Test
    @DisplayName("Should preserve file extension")
    void store_shouldPreserveExtension() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "document.pdf", "application/pdf", 1L);
        assertThat(storagePath).endsWith(".pdf");
    }

    @Test
    @DisplayName("Should load content from processing bucket")
    void loadFromProcessing_shouldReturnContent() {
        var content = "test content".getBytes();
        var storagePath = storageService.store(content, "test.txt", "text/plain", 1L);
        var processingPath = storageService.copyToProcessing(storagePath, 42L);
        var loaded = storageService.loadFromProcessing(processingPath);
        assertThat(loaded).isEqualTo(content);
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent from processing")
    void loadFromProcessing_shouldThrowWhenNotFound() {
        assertThatThrownBy(() -> storageService.loadFromProcessing("pending/non_existent.txt"))
                .isInstanceOf(StorageException.class);
    }
}