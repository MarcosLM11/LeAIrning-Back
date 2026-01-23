package com.marcos.documentsservice.service;

import com.marcos.documentsservice.entity.Document;
import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import com.marcos.documentsservice.entity.dto.DocumentDTO;
import com.marcos.documentsservice.exception.DocumentNotFoundException;
import com.marcos.documentsservice.repository.DocumentRepository;
import com.marcos.documentsservice.storage.StorageService;
import com.marcos.documentsservice.util.DocumentMapper;
import com.marcos.documentsservice.validator.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private FileValidator fileValidator;

    @Mock
    private DocumentProcessorService documentProcessorService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private Document document;
    private DocumentDTO documentDTO;
    private MultipartFile multipartFile;
    private Long userId;
    private Long documentId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        documentId = 1L;

        document = new Document();
        document.setId(documentId);
        document.setUserId(userId);
        document.setOriginalFilename("test.pdf");
        document.setStoredFilename("uuid-test.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(1024L);
        document.setDocumentType(DocumentType.PDF);
        document.setStatus(ProcessingStatus.UPLOADED);
        document.setStoragePath("/storage/documents/1/2026/01/uuid-test.pdf");

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

        multipartFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
    }

    @Test
    @DisplayName("Should upload single document successfully")
    void shouldUploadSingleDocumentSuccessfully() {
        // Given
        var storagePath = "/storage/documents/1/2026/01/uuid-test.pdf";
        doNothing().when(fileValidator).validate(multipartFile);
        when(storageService.store(any(byte[].class), eq("test.pdf"), eq("application/pdf"), eq(userId)))
                .thenReturn(storagePath);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDTO(document)).thenReturn(documentDTO);

        // When
        var result = documentService.uploadDocuments(List.of(multipartFile), userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(documentDTO.originalFilename(), result.getFirst().originalFilename());
        assertEquals(documentDTO.contentType(), result.getFirst().contentType());
        verify(fileValidator).validate(multipartFile);
        verify(storageService).store(any(byte[].class), eq("test.pdf"), eq("application/pdf"), eq(userId));
        verify(documentRepository).save(any(Document.class));
        verify(documentMapper).toDTO(document);
        verify(documentProcessorService).processDocumentAsync(any(Document.class));
    }

    @Test
    @DisplayName("Should upload multiple documents successfully")
    void shouldUploadMultipleDocumentsSuccessfully() {
        // Given
        MultipartFile file2 = new MockMultipartFile(
                "file2",
                "test2.txt",
                "text/plain",
                "test content 2".getBytes()
        );

        Document document2 = new Document();
        document2.setId(2L);
        document2.setUserId(userId);
        document2.setOriginalFilename("test2.txt");

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

        doNothing().when(fileValidator).validate(any(MultipartFile.class));
        when(storageService.store(any(byte[].class), anyString(), anyString(), eq(userId)))
                .thenReturn("/storage/path1")
                .thenReturn("/storage/path2");
        when(documentRepository.save(any(Document.class)))
                .thenReturn(document)
                .thenReturn(document2);
        when(documentMapper.toDTO(document)).thenReturn(documentDTO);
        when(documentMapper.toDTO(document2)).thenReturn(documentDTO2);

        // When
        List<DocumentDTO> result = documentService.uploadDocuments(
                List.of(multipartFile, file2),
                userId
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(fileValidator, times(2)).validate(any(MultipartFile.class));
        verify(storageService, times(2)).store(any(byte[].class), anyString(), anyString(), eq(userId));
        verify(documentRepository, times(2)).save(any(Document.class));
        verify(documentProcessorService, times(2)).processDocumentAsync(any(Document.class));
    }

    @Test
    @DisplayName("Should get document by id successfully when user owns document")
    void shouldGetDocumentByIdSuccessfullyWhenUserOwnsDocument() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentMapper.toDTO(document)).thenReturn(documentDTO);

        // When
        DocumentDTO result = documentService.getDocumentById(documentId, userId);

        // Then
        assertNotNull(result);
        assertEquals(documentDTO.id(), result.id());
        assertEquals(documentDTO.originalFilename(), result.originalFilename());

        verify(documentRepository).findById(documentId);
        verify(documentMapper).toDTO(document);
    }

    @Test
    @DisplayName("Should throw DocumentNotFoundException when document not found")
    void shouldThrowDocumentNotFoundExceptionWhenDocumentNotFound() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DocumentNotFoundException.class,
                () -> documentService.getDocumentById(documentId, userId));

        verify(documentRepository).findById(documentId);
        verify(documentMapper, never()).toDTO(any());
    }

    @Test
    @DisplayName("Should list user documents with pagination")
    void shouldListUserDocumentsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<Document> documents = Collections.singletonList(document);
        Page<Document> documentPage = new PageImpl<>(documents, pageable, 1);

        when(documentRepository.findByUserId(userId, pageable)).thenReturn(documentPage);
        when(documentMapper.toDTO(document)).thenReturn(documentDTO);

        // When
        Page<DocumentDTO> result = documentService.getUserDocuments(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(documentDTO.originalFilename(), result.getContent().getFirst().originalFilename());

        verify(documentRepository).findByUserId(userId, pageable);
        verify(documentMapper).toDTO(document);
    }

    @Test
    @DisplayName("Should delete document successfully when user owns document")
    void shouldDeleteDocumentSuccessfullyWhenUserOwnsDocument() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doNothing().when(storageService).delete(document.getStoragePath());
        doNothing().when(documentRepository).delete(document);

        // When
        documentService.deleteDocument(documentId, userId);

        // Then
        verify(documentRepository).findById(documentId);
        verify(storageService).delete(document.getStoragePath());
        verify(documentRepository).delete(document);
    }

    @Test
    @DisplayName("Should delete multiple documents successfully")
    void shouldDeleteMultipleDocumentsSuccessfully() {
        // Given
        Document document2 = new Document();
        document2.setId(2L);
        document2.setUserId(userId);
        document2.setStoragePath("/storage/path2");

        List<Long> documentIds = Arrays.asList(1L, 2L);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(document2));
        doNothing().when(storageService).delete(any());
        doNothing().when(documentRepository).delete(any());

        // When
        documentService.deleteDocuments(documentIds, userId);

        // Then
        verify(documentRepository, times(2)).findById(any());
        verify(storageService, times(2)).delete(any());
        verify(documentRepository, times(2)).delete(any());
    }

    @Test
    @DisplayName("Should download document successfully when user owns document")
    void shouldDownloadDocumentSuccessfullyWhenUserOwnsDocument() {
        // Given
        var fileContent = "test content".getBytes();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(storageService.load(document.getStoragePath())).thenReturn(fileContent);

        // When
        var result = documentService.downloadDocument(documentId, userId);

        // Then
        assertNotNull(result);
        assertArrayEquals(fileContent, result);
        verify(documentRepository).findById(documentId);
        verify(storageService).load(document.getStoragePath());
    }

    @Test
    @DisplayName("Should get document by id for processing without user validation")
    void shouldGetDocumentByIdForProcessingWithoutUserValidation() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        // When
        Document result = documentService.getDocumentForProcessing(documentId);

        // Then
        assertNotNull(result);
        assertEquals(document.getId(), result.getId());
        assertEquals(document.getOriginalFilename(), result.getOriginalFilename());

        verify(documentRepository).findById(documentId);
    }
}