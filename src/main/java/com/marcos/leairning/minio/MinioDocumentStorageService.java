package com.marcos.leairning.minio;

import com.marcos.leairning.documents.Document;
import com.marcos.leairning.exception.StorageOperationException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class MinioDocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioDocumentStorageService.class);

    private final MinioClient client;
    private final MinioProperties properties;

    public MinioDocumentStorageService(MinioClient client, MinioProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public String store(byte[] content, Document document) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Cannot store empty content");
        }
        
        var extension = getExtension(document.getFileName());
        var uniqueFileName = document.getFileName() + "-" + document.getId() + extension;
        var objectPath = document.getUserId() + "/" + uniqueFileName;
        log.info("Storing file to path: {}", objectPath);
        
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getDocumentsBucket())
                    .object(objectPath)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(document.getContentType() != null ? document.getContentType() : "application/octet-stream")
                    .build());
            log.info("File stored successfully: {}", objectPath);
            return objectPath;
        
        } catch (Exception e) {
            throw new StorageOperationException("store file in MinIO", e);
        }
    }

    public byte[] load(String objectPath) {
        log.info("Loading file from path: {}", objectPath);
        
        try (var stream = loadAsStream(objectPath)) {
            return stream.readAllBytes();
        
        } catch (Exception e) {
            throw new StorageOperationException("load file from MinIO", e);
        }
    }

    public InputStream loadAsStream(String objectPath) {
        log.info("Loading file as stream from path: {}", objectPath);
        
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(properties.getDocumentsBucket())
                    .object(objectPath)
                    .build());
        
        } catch (Exception e) {
            throw new StorageOperationException("load file from MinIO", e);
        }
    }

    public void delete(String objectPath) {
        log.info("Deleting file from path: {}", objectPath);
        
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getDocumentsBucket())
                    .object(objectPath)
                    .build());
            log.info("File deleted successfully: {}", objectPath);
        
        } catch (Exception e) {
            throw new StorageOperationException("delete file from MinIO", e);
        }
    }

    private String getExtension(String filename) {
        var lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}