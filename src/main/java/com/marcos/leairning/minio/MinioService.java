package com.marcos.leairning.minio;

import com.marcos.leairning.documents.Document;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.messages.Item;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinioService {

    private static final String PENDING_PREFIX = "pending/";
    private static final String PROCESSED_PREFIX = "processed/";
    private static final String FAILED_PREFIX = "failed/";

    MinioClient client;
    MinioProperties properties;

    public String store(byte[] content, Document document) {

        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Cannot store empty content");
        }

        val extension = getExtension(document.getFileName());
        val uniqueFileName = document.getFileName() + "-" + document.getId() + extension;
        val objectPath = document.getUserId() + "/" + uniqueFileName;

        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getDocumentsBucket())
                    .object(objectPath)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(document.getContentType() != null ? document.getContentType() : "application/octet-stream")
                    .build());

            return objectPath;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store file in MinIO", e);
        }
    }

    public byte[] load(String objectPath) {
        try (val stream = loadAsStream(objectPath)) {
            return stream.readAllBytes();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load file from MinIO", e);
        }
    }

    public InputStream loadAsStream(String objectPath) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(properties.getDocumentsBucket())
                    .object(objectPath)
                    .build());

        } catch (Exception e) {
            throw new RuntimeException("Failed to load file from MinIO", e);
        }
    }

    public void delete(String objectPath) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getDocumentsBucket())
                    .object(objectPath)
                    .build());

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    public String copyTo(String objectPath, UUID documentId) {
        var filename = objectPath.substring(objectPath.lastIndexOf('/') + 1);
        var processingPath = PENDING_PREFIX + documentId + "_" + filename;

        try {
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(properties.getProcessingBucket())
                    .object(processingPath)
                    .source(CopySource.builder()
                            .bucket(properties.getDocumentsBucket())
                            .object(objectPath)
                            .build())
                    .build());
            return processingPath;

        } catch (Exception e) {
            throw new RuntimeException("Failed to copy file in MinIO", e);
        }

    }

    public List<String> listPendingFiles() {
        val pendingFiles = new ArrayList<String>();

        try {
            var results = client.listObjects(ListObjectsArgs.builder()
                    .bucket(properties.getProcessingBucket())
                    .prefix(PENDING_PREFIX)
                    .build());

            for (var result : results) {

                Item item = result.get();

                if (!item.isDir()) {
                    pendingFiles.add(item.objectName());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to list pending files", e);
        }

        return pendingFiles;
    }

    public byte[] loadFromProcessing(String processingPath) {

        try (val stream = client.getObject(GetObjectArgs.builder()
                .bucket(properties.getProcessingBucket())
                .object(processingPath)
                .build())) {

            return stream.readAllBytes();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load from processing", e);
        }
    }

    public void markProcessed(String processingPath, boolean success) {
        val filename = processingPath.substring(PENDING_PREFIX.length());
        val targetPath = (success ? PROCESSED_PREFIX : FAILED_PREFIX) + filename;

        try {
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(properties.getProcessingBucket())
                    .object(targetPath)
                    .source(CopySource.builder()
                            .bucket(properties.getProcessingBucket())
                            .object(processingPath)
                            .build())
                    .build());

            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getProcessingBucket())
                    .object(processingPath)
                    .build());


        } catch (Exception e) {
            throw new RuntimeException("Failed to mark as processed", e);
        }
    }

    private String getExtension(String filename) {
        var lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

}
