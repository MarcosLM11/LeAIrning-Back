package com.marcos.leairning.minio;

import com.marcos.leairning.exception.StorageOperationException;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.CopySource;
import io.minio.Result;
import io.minio.RemoveObjectArgs;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class MinioProcessingPipelineService {

    private static final Logger log = LoggerFactory.getLogger(MinioProcessingPipelineService.class);
    private static final String PENDING_PREFIX = "pending/";
    private static final String PROCESSED_PREFIX = "processed/";
    private static final String FAILED_PREFIX = "failed/";

    private final MinioClient client;
    private final MinioProperties properties;

    public MinioProcessingPipelineService(MinioClient client, MinioProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public void copyToProcessing(String objectPath, UUID documentId) {
        var filename = objectPath.substring(objectPath.lastIndexOf('/') + 1);
        var processingPath = PENDING_PREFIX + documentId + "_" + filename;
        log.info("Copying file to processing: {} -> {}", objectPath, processingPath);
        
        try {
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(properties.getProcessingBucket())
                    .object(processingPath)
                    .source(CopySource.builder()
                            .bucket(properties.getDocumentsBucket())
                            .object(objectPath)
                            .build())
                    .build());
            log.info("File copied successfully to: {}", processingPath);
        
        } catch (Exception e) {
            throw new StorageOperationException("copy file to processing bucket", e);
        }
    }

    public List<String> listPendingFiles() {
        log.info("Listing pending files");
        
        try {
            var results = client.listObjects(ListObjectsArgs.builder()
                    .bucket(properties.getProcessingBucket())
                    .prefix(PENDING_PREFIX)
                    .build());
            
            var pendingFiles = StreamSupport.stream(results.spliterator(), false)
                    .map(this::getItemSafely)
                    .filter(item -> !item.isDir())
                    .map(Item::objectName)
                    .toList();
            log.info("Found {} pending files", pendingFiles.size());
            
            return pendingFiles;
            
        } catch (Exception e) {
            throw new StorageOperationException("list pending files", e);
        }
    }

    private Item getItemSafely(Result<Item> result) {
        
        try {
            return result.get();
        
        } catch (Exception e) {
            throw new StorageOperationException("get item from result", e);
        }
    }

    public byte[] loadFromProcessing(String processingPath) {
        log.info("Loading file from processing: {}", processingPath);
        
        try (var stream = client.getObject(GetObjectArgs.builder()
                .bucket(properties.getProcessingBucket())
                .object(processingPath)
                .build())) {
        
            return stream.readAllBytes();
        
        } catch (Exception e) {
            throw new StorageOperationException("load from processing bucket", e);
        }
    }

    public void markProcessed(String processingPath, boolean success) {
        var filename = processingPath.substring(PENDING_PREFIX.length());
        var targetPath = (success ? PROCESSED_PREFIX : FAILED_PREFIX) + filename;
        log.atInfo().log("Marking file as {}: {} -> {}", success ? "processed" : "failed", processingPath, targetPath);
        
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
            log.atInfo().log("File marked as {} successfully", success ? "processed" : "failed");
            
        } catch (Exception e) {
            throw new StorageOperationException("mark file as processed", e);
        }
    }
}