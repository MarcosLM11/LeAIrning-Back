package com.marcos.leairning.minio;

import com.marcos.leairning.exception.StorageOperationException;
import io.minio.*;
import io.minio.messages.Item;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Flogger
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinioProcessingPipelineService {

    private static final String PENDING_PREFIX = "pending/";
    private static final String PROCESSED_PREFIX = "processed/";
    private static final String FAILED_PREFIX = "failed/";

    MinioClient client;
    MinioProperties properties;

    public String copyToProcessing(String objectPath, UUID documentId) {
        val filename = objectPath.substring(objectPath.lastIndexOf('/') + 1);
        val processingPath = PENDING_PREFIX + documentId + "_" + filename;
        log.atFine().log("Copying file to processing: %s -> %s", objectPath, processingPath);
        
        try {
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(properties.getProcessingBucket())
                    .object(processingPath)
                    .source(CopySource.builder()
                            .bucket(properties.getDocumentsBucket())
                            .object(objectPath)
                            .build())
                    .build());
            log.atFine().log("File copied successfully to: %s", processingPath);
        
            return processingPath;
        
        } catch (Exception e) {
            throw new StorageOperationException("copy file to processing bucket", e);
        }
    }

    public List<String> listPendingFiles() {
        log.atFine().log("Listing pending files");
        
        try {
            val results = client.listObjects(ListObjectsArgs.builder()
                    .bucket(properties.getProcessingBucket())
                    .prefix(PENDING_PREFIX)
                    .build());
            
            val pendingFiles = StreamSupport.stream(results.spliterator(), false)
                    .map(this::getItemSafely)
                    .filter(item -> !item.isDir())
                    .map(Item::objectName)
                    .toList();
            log.atFine().log("Found %d pending files", pendingFiles.size());
            
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
        log.atFine().log("Loading file from processing: %s", processingPath);
        
        try (val stream = client.getObject(GetObjectArgs.builder()
                .bucket(properties.getProcessingBucket())
                .object(processingPath)
                .build())) {
        
            return stream.readAllBytes();
        
        } catch (Exception e) {
            throw new StorageOperationException("load from processing bucket", e);
        }
    }

    public void markProcessed(String processingPath, boolean success) {
        val filename = processingPath.substring(PENDING_PREFIX.length());
        val targetPath = (success ? PROCESSED_PREFIX : FAILED_PREFIX) + filename;
        log.atInfo().log("Marking file as %s: %s -> %s", success ? "processed" : "failed", processingPath, targetPath);
        
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
            log.atInfo().log("File marked as %s successfully", success ? "processed" : "failed");
            
        } catch (Exception e) {
            throw new StorageOperationException("mark file as processed", e);
        }
    }
}