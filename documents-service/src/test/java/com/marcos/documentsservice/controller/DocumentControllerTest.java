package com.marcos.documentsservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import com.marcos.documentsservice.entity.dto.BatchDeleteRequest;
import com.marcos.documentsservice.entity.dto.DocumentDTO;
import com.marcos.documentsservice.exception.DocumentNotFoundException;
import com.marcos.documentsservice.exception.UnauthorizedAccessException;
import com.marcos.documentsservice.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = DocumentController.class,
    excludeAutoConfiguration = JpaRepositoriesAutoConfiguration.class
)
@Import(DocumentControllerAdvice.class)
@DisplayName("DocumentController API Tests")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentService documentService;

    private DocumentDTO documentDTO;
    private Long userId;
    private Long documentId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        documentId = 1L;

        documentDTO = new DocumentDTO(
                documentId,
                "test.pdf",
                "application/pdf",
                1024L,
                DocumentType.PDF,
                ProcessingStatus.UPLOADED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should upload single document successfully")
    void shouldUploadSingleDocumentSuccessfully() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(documentService.uploadDocuments(any(), eq(userId)))
                .thenReturn(List.of(documentDTO));

        // When & Then
        mockMvc.perform(multipart("/api/1.0/documents")
                        .file(file)
                        .header("X-User-Id", userId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documents", hasSize(1)))
                .andExpect(jsonPath("$.documents[0].id").value(documentId))
                .andExpect(jsonPath("$.documents[0].originalFilename").value("test.pdf"))
                .andExpect(jsonPath("$.documents[0].status").value("UPLOADED"))
                .andExpect(jsonPath("$.message").value("Documents uploaded successfully. Processing started."));

        verify(documentService).uploadDocuments(any(), eq(userId));
    }

    @Test
    @DisplayName("Should upload multiple documents successfully")
    void shouldUploadMultipleDocumentsSuccessfully() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "test1.pdf",
                "application/pdf",
                "test content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "test2.txt",
                "text/plain",
                "test content 2".getBytes()
        );

        DocumentDTO documentDTO2 = new DocumentDTO(
                2L,
                "test2.txt",
                "text/plain",
                1024L,
                DocumentType.TXT,
                ProcessingStatus.UPLOADED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(documentService.uploadDocuments(any(), eq(userId)))
                .thenReturn(List.of(documentDTO, documentDTO2));

        // When & Then
        mockMvc.perform(multipart("/api/1.0/documents")
                        .file(file1)
                        .file(file2)
                        .header("X-User-Id", userId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documents", hasSize(2)))
                .andExpect(jsonPath("$.documents[0].originalFilename").value("test.pdf"))
                .andExpect(jsonPath("$.documents[1].originalFilename").value("test2.txt"));

        verify(documentService).uploadDocuments(any(), eq(userId));
    }

    @Test
    @DisplayName("Should return 400 when no files provided")
    void shouldReturn400WhenNoFilesProvided() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/1.0/documents")
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest());

        verify(documentService, never()).uploadDocuments(any(), any());
    }

    @Test
    @DisplayName("Should get document by id successfully")
    void shouldGetDocumentByIdSuccessfully() throws Exception {
        // Given
        when(documentService.getDocumentById(documentId, userId))
                .thenReturn(documentDTO);

        // When & Then
        mockMvc.perform(get("/api/1.0/documents/{documentId}", documentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId))
                .andExpect(jsonPath("$.originalFilename").value("test.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.documentType").value("PDF"))
                .andExpect(jsonPath("$.status").value("UPLOADED"));

        verify(documentService).getDocumentById(documentId, userId);
    }

    @Test
    @DisplayName("Should return 404 when document not found")
    void shouldReturn404WhenDocumentNotFound() throws Exception {
        // Given
        when(documentService.getDocumentById(documentId, userId))
                .thenThrow(new DocumentNotFoundException("Document not found"));

        // When & Then
        mockMvc.perform(get("/api/1.0/documents/{documentId}", documentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound());

        verify(documentService).getDocumentById(documentId, userId);
    }

    @Test
    @DisplayName("Should return 403 when user does not own document")
    void shouldReturn403WhenUserDoesNotOwnDocument() throws Exception {
        // Given
        when(documentService.getDocumentById(documentId, userId))
                .thenThrow(new UnauthorizedAccessException("Access denied"));

        // When & Then
        mockMvc.perform(get("/api/1.0/documents/{documentId}", documentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isForbidden());

        verify(documentService).getDocumentById(documentId, userId);
    }

    @Test
    @DisplayName("Should list user documents with pagination")
    void shouldListUserDocumentsWithPagination() throws Exception {
        // Given
        Page<DocumentDTO> documentPage = new PageImpl<>(
                List.of(documentDTO),
                PageRequest.of(0, 20),
                1
        );

        when(documentService.getUserDocuments(eq(userId), any()))
                .thenReturn(documentPage);

        // When & Then
        mockMvc.perform(get("/api/1.0/documents")
                        .header("X-User-Id", userId)
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(documentId))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(documentService).getUserDocuments(eq(userId), any());
    }

    @Test
    @DisplayName("Should download document successfully")
    void shouldDownloadDocumentSuccessfully() throws Exception {
        // Given
        byte[] fileContent = "test content".getBytes();
        when(documentService.downloadDocument(documentId, userId))
                .thenReturn(fileContent);
        when(documentService.getDocumentById(documentId, userId))
                .thenReturn(documentDTO);

        // When & Then
        mockMvc.perform(get("/api/1.0/documents/{documentId}/download", documentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "form-data; name=\"attachment\"; filename=\"test.pdf\""))
                .andExpect(content().bytes(fileContent));

        verify(documentService).downloadDocument(documentId, userId);
        verify(documentService).getDocumentById(documentId, userId);
    }

    @Test
    @DisplayName("Should delete document successfully")
    void shouldDeleteDocumentSuccessfully() throws Exception {
        // Given
        doNothing().when(documentService).deleteDocument(documentId, userId);

        // When & Then
        mockMvc.perform(delete("/api/1.0/documents/{documentId}", documentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(documentId, userId);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent document")
    void shouldReturn404WhenDeletingNonExistentDocument() throws Exception {
        // Given
        doThrow(new DocumentNotFoundException("Document not found"))
                .when(documentService).deleteDocument(documentId, userId);

        // When & Then
        mockMvc.perform(delete("/api/1.0/documents/{documentId}", documentId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound());

        verify(documentService).deleteDocument(documentId, userId);
    }

    @Test
    @DisplayName("Should delete multiple documents successfully")
    void shouldDeleteMultipleDocumentsSuccessfully() throws Exception {
        // Given
        BatchDeleteRequest request = new BatchDeleteRequest(Arrays.asList(1L, 2L, 3L));
        doNothing().when(documentService).deleteDocuments(any(), eq(userId));

        // When & Then
        mockMvc.perform(delete("/api/1.0/documents/batch")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(3))
                .andExpect(jsonPath("$.failed").value(0));

        verify(documentService).deleteDocuments(request.documentIds(), userId);
    }

    @Test
    @DisplayName("Should return 400 for empty batch delete request")
    void shouldReturn400ForEmptyBatchDeleteRequest() throws Exception {
        // Given
        BatchDeleteRequest request = new BatchDeleteRequest(List.of());

        // When & Then
        mockMvc.perform(delete("/api/1.0/documents/batch")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(documentService, never()).deleteDocuments(any(), any());
    }

    @Test
    @DisplayName("Should return 401 when user id header is missing")
    void shouldReturn401WhenUserIdHeaderIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/1.0/documents"))
                .andExpect(status().isUnauthorized());

        verify(documentService, never()).getUserDocuments(any(), any());
    }

    @Test
    @DisplayName("Should filter documents by status")
    void shouldFilterDocumentsByStatus() throws Exception {
        // Given
        Page<DocumentDTO> documentPage = new PageImpl<>(
                List.of(documentDTO),
                PageRequest.of(0, 20),
                1
        );

        when(documentService.getUserDocumentsByStatus(eq(userId), eq(ProcessingStatus.COMPLETED), any()))
                .thenReturn(documentPage);

        // When & Then
        mockMvc.perform(get("/api/1.0/documents")
                        .header("X-User-Id", userId)
                        .param("status", "COMPLETED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("UPLOADED"));

        verify(documentService).getUserDocumentsByStatus(eq(userId), eq(ProcessingStatus.COMPLETED), any());
    }

    @Test
    @DisplayName("Should filter documents by document type")
    void shouldFilterDocumentsByDocumentType() throws Exception {
        // Given
        Page<DocumentDTO> documentPage = new PageImpl<>(
                List.of(documentDTO),
                PageRequest.of(0, 20),
                1
        );

        when(documentService.getUserDocumentsByType(eq(userId), eq(DocumentType.PDF), any()))
                .thenReturn(documentPage);

        // When & Then
        mockMvc.perform(get("/api/1.0/documents")
                        .header("X-User-Id", userId)
                        .param("type", "PDF")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].documentType").value("PDF"));

        verify(documentService).getUserDocumentsByType(eq(userId), eq(DocumentType.PDF), any());
    }

    @Test
    @DisplayName("Should return user statistics")
    void shouldReturnUserStatistics() throws Exception {
        // Given
        when(documentService.getUserDocumentCount(userId)).thenReturn(10L);
        when(documentService.getUserStorageUsed(userId)).thenReturn(1024000L);

        // When & Then
        mockMvc.perform(get("/api/1.0/documents/statistics")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocuments").value(10))
                .andExpect(jsonPath("$.storageUsed").value(1024000));

        verify(documentService).getUserDocumentCount(userId);
        verify(documentService).getUserStorageUsed(userId);
    }
}