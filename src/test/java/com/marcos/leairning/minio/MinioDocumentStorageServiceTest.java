package com.marcos.leairning.minio;

import com.marcos.leairning.documents.Document;
import com.marcos.leairning.exception.StorageOperationException;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MinioDocumentStorageServiceTest {

    MinioClient client;
    MinioProperties properties;
    MinioDocumentStorageService service;

    @BeforeEach
    void setUp() {
        client = mock(MinioClient.class);
        properties = mock(MinioProperties.class);
        when(properties.getDocumentsBucket()).thenReturn("documents");
        service = new MinioDocumentStorageService(client, properties);
    }

    @Test
    void store_validContent_returnsObjectPath() throws Exception {
        val doc = createDocument("test.pdf");
        val content = "pdf content".getBytes();
        val result = service.store(content, doc);
        assertTrue(result.contains(doc.getUserId().toString()));
        assertTrue(result.contains(".pdf"));
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void store_emptyContent_throwsIllegalArgument() {
        val doc = createDocument("test.pdf");
        assertThrows(IllegalArgumentException.class, () -> service.store(new byte[0], doc));
    }

    @Test
    void store_nullContent_throwsIllegalArgument() {
        val doc = createDocument("test.pdf");
        assertThrows(IllegalArgumentException.class, () -> service.store(null, doc));
    }

    @Test
    void store_minioFailure_throwsStorageOperation() throws Exception {
        val doc = createDocument("test.pdf");
        doThrow(new RuntimeException("minio error")).when(client).putObject(any(PutObjectArgs.class));
        assertThrows(StorageOperationException.class, () -> service.store("data".getBytes(), doc));
    }

    @Test
    void load_success_returnsByteArray() throws Exception {
        val response = mock(GetObjectResponse.class);
        when(response.readAllBytes()).thenReturn("content".getBytes());
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(response);
        val result = service.load("path/file.pdf");
        assertArrayEquals("content".getBytes(), result);
    }

    @Test
    void load_minioFailure_throwsStorageOperation() throws Exception {
        when(client.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("error"));
        assertThrows(StorageOperationException.class, () -> service.load("path/file.pdf"));
    }

    @Test
    void delete_success_callsRemoveObject() throws Exception {
        service.delete("path/file.pdf");
        verify(client).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void delete_minioFailure_throwsStorageOperation() throws Exception {
        doThrow(new RuntimeException("error")).when(client).removeObject(any(RemoveObjectArgs.class));
        assertThrows(StorageOperationException.class, () -> service.delete("path/file.pdf"));
    }

    private Document createDocument(String fileName) {
        val doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setUserId(UUID.randomUUID());
        doc.setFileName(fileName);
        doc.setContentType("application/pdf");
        return doc;
    }
}
