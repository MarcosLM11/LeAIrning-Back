package com.marcos.documentsservice.storage;

import io.minio.*;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioStorageService.class);
    private static final String PENDING_PREFIX = "pending/";
    private static final String PROCESSED_PREFIX = "processed/";
    private static final String FAILED_PREFIX = "failed/";

    private final MinioClient minioClient;
    private final String documentsBucket;
    private final String processingBucket;

    public MinioStorageService(
            MinioClient minioClient,
            @Value("${storage.minio.documents-bucket}") String documentsBucket,
            @Value("${storage.minio.processing-bucket}") String processingBucket) {
        this.minioClient = minioClient;
        this.documentsBucket = documentsBucket;
        this.processingBucket = processingBucket;
    }

    @Override
    public String store(byte[] content, String filename, String contentType, Long userId) {
        if (content == null || content.length == 0) {
            throw new StorageException("Cannot store empty content");
        }
        var now = LocalDate.now();
        var sanitizedFilename = sanitizeFilename(filename);
        var extension = getExtension(sanitizedFilename);
        var uniqueFilename = UUID.randomUUID() + extension;
        var objectPath = String.format("%d/%d/%02d/%s",
                userId, now.getYear(), now.getMonthValue(), uniqueFilename);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(documentsBucket)
                    .object(objectPath)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
            LOGGER.info("Stored document: {}/{}", documentsBucket, objectPath);
            return objectPath;
        } catch (Exception e) {
            LOGGER.error("Failed to store document in MinIO", e);
            throw new StorageException("Failed to store document", e);
        }
    }

    @Override
    public byte[] load(String storagePath) {
        try (var stream = loadAsStream(storagePath)) {
            return stream.readAllBytes();
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to load document from MinIO: {}", storagePath, e);
            throw new StorageException("Failed to load document", e);
        }
    }

    @Override
    public InputStream loadAsStream(String storagePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(documentsBucket)
                    .object(storagePath)
                    .build());
        } catch (Exception e) {
            LOGGER.error("Failed to load document stream from MinIO: {}", storagePath, e);
            throw new StorageException("Failed to load document stream", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(documentsBucket)
                    .object(storagePath)
                    .build());
            LOGGER.info("Deleted document: {}/{}", documentsBucket, storagePath);
        } catch (Exception e) {
            LOGGER.error("Failed to delete document from MinIO: {}", storagePath, e);
            throw new StorageException("Failed to delete document", e);
        }
    }

    @Override
    public String copyToProcessing(String storagePath, Long documentId) {
        var filename = storagePath.substring(storagePath.lastIndexOf('/') + 1);
        var processingPath = PENDING_PREFIX + documentId + "_" + filename;
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(processingBucket)
                    .object(processingPath)
                    .source(CopySource.builder()
                            .bucket(documentsBucket)
                            .object(storagePath)
                            .build())
                    .build());
            LOGGER.info("Copied to processing: {}", processingPath);
            return processingPath;
        } catch (Exception e) {
            LOGGER.error("Failed to copy document to processing bucket", e);
            throw new StorageException("Failed to copy to processing", e);
        }
    }

    @Override
    public List<String> listPendingFiles() {
        var pendingFiles = new ArrayList<String>();
        try {
            var results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(processingBucket)
                    .prefix(PENDING_PREFIX)
                    .build());
            for (var result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    pendingFiles.add(item.objectName());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to list pending files", e);
            throw new StorageException("Failed to list pending files", e);
        }
        return pendingFiles;
    }

    @Override
    public byte[] loadFromProcessing(String processingPath) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(processingBucket)
                .object(processingPath)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            LOGGER.error("Failed to load from processing bucket: {}", processingPath, e);
            throw new StorageException("Failed to load from processing", e);
        }
    }

    @Override
    public void markProcessed(String processingPath, boolean success) {
        var filename = processingPath.substring(PENDING_PREFIX.length());
        var targetPath = (success ? PROCESSED_PREFIX : FAILED_PREFIX) + filename;
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(processingBucket)
                    .object(targetPath)
                    .source(CopySource.builder()
                            .bucket(processingBucket)
                            .object(processingPath)
                            .build())
                    .build());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(processingBucket)
                    .object(processingPath)
                    .build());
            LOGGER.info("Marked as {}: {}", success ? "processed" : "failed", targetPath);
        } catch (Exception e) {
            LOGGER.error("Failed to mark document as processed", e);
            throw new StorageException("Failed to mark as processed", e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getExtension(String filename) {
        var lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}