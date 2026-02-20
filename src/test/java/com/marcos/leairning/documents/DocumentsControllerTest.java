package com.marcos.leairning.documents;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentsControllerTest {

    DocumentsService service;
    DocumentsController controller;

    @BeforeEach
    void setUp() {
        service = mock(DocumentsService.class);
        controller = new DocumentsController(service);
    }

    @Test
    void getDocuments_returnsPage() {
        val userId = UUID.randomUUID();
        val pageable = PageRequest.of(0, 20);
        val doc = new DocumentResponseDTO(UUID.randomUUID(), userId, "file.pdf", "application/pdf", 1024L, "/path");
        val page = new PageImpl<>(List.of(doc));
        when(service.getDocuments(userId, pageable)).thenReturn(page);
        val result = controller.getDocuments(userId, pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals(doc, result.getContent().getFirst());
    }

    @Test
    void upload_returnsDocumentList() {
        val userId = UUID.randomUUID();
        val file = mock(MultipartFile.class);
        val doc = new DocumentResponseDTO(UUID.randomUUID(), userId, "file.pdf", "application/pdf", 1024L, "/path");
        when(service.upload(userId, List.of(file))).thenReturn(List.of(doc));
        val result = controller.upload(userId, List.of(file));
        assertEquals(1, result.size());
        assertEquals(doc, result.getFirst());
    }

    @Test
    void getDocument_returnsDocument() {
        val userId = UUID.randomUUID();
        val docId = UUID.randomUUID();
        val doc = new DocumentResponseDTO(docId, userId, "file.pdf", "application/pdf", 1024L, "/path");
        when(service.getDocument(userId, docId)).thenReturn(doc);
        val result = controller.getDocument(userId, docId);
        assertEquals(doc, result);
    }

    @Test
    void downloadDocument_returnsResponseWithHeaders() {
        val userId = UUID.randomUUID();
        val docId = UUID.randomUUID();
        val doc = new DocumentResponseDTO(docId, userId, "file.pdf", "application/pdf", 1024L, "/path");
        val content = "file content".getBytes();
        when(service.getDocument(userId, docId)).thenReturn(doc);
        when(service.downloadDocument(userId, docId)).thenReturn(content);
        val response = controller.downloadDocument(userId, docId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(content, response.getBody());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("file.pdf"));
        assertEquals(content.length, response.getHeaders().getContentLength());
    }

    @Test
    void deleteDocument_callsService() {
        val userId = UUID.randomUUID();
        val docId = UUID.randomUUID();
        controller.deleteDocument(userId, docId);
        verify(service).deleteDocument(userId, docId);
    }

    @Test
    void deleteDocuments_callsServiceWithIds() {
        val userId = UUID.randomUUID();
        val ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        controller.deleteDocuments(userId, ids);
        verify(service).deleteDocuments(userId, ids);
    }
}
