package com.marcos.leairning.minio;

import com.marcos.leairning.documents.Document;
import com.marcos.leairning.exception.StorageOperationException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Flogger
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinioDocumentStorageService {

    MinioClient client;
    MinioProperties properties;

    public String store(byte[] content, Document document) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Cannot store empty content");
        }
        
        val extension = getExtension(document.getFileName());
        val uniqueFileName = document.getFileName() + "-" + document.getId() + extension;
        val objectPath = document.getUserId() + "/" + uniqueFileName;
        log.atFine().log("Storing file to path: %s", objectPath);
        
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getDocumentsBucket())
                    .object(objectPath)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(document.getContentType() != null ? document.getContentType() : "application/octet-stream")
                    .build());
            log.atFine().log("File stored successfully: %s", objectPath);
        
            return objectPath;
        
        } catch (Exception e) {
            throw new StorageOperationException("store file in MinIO", e);
        }
    }

    public byte[] load(String objectPath) {
        log.atFine().log("Loading file from path: %s", objectPath);
        
        try (val stream = loadAsStream(objectPath)) {
            return stream.readAllBytes();
        
        } catch (Exception e) {
            throw new StorageOperationException("load file from MinIO", e);
        }
    }

    public InputStream loadAsStream(String objectPath) {
        log.atFine().log("Loading file as stream from path: %s", objectPath);
        
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
        log.atInfo().log("Deleting file from path: %s", objectPath);
        
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getDocumentsBucket())
                    .object(objectPath)
                    .build());
            log.atInfo().log("File deleted successfully: %s", objectPath);
        
        } catch (Exception e) {
            throw new StorageOperationException("delete file from MinIO", e);
        }
    }

    private String getExtension(String filename) {
        val lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}