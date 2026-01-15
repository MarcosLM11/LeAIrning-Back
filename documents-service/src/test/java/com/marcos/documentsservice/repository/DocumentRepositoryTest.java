package com.marcos.documentsservice.repository;

import com.marcos.documentsservice.TestcontainersConfiguration;
import com.marcos.documentsservice.entity.Document;
import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import com.marcos.documentsservice.config.JpaAuditingConfiguration;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("DocumentRepository Integration Tests")
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    private Document document1;
    private Document document2;
    private Document document3;
    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();

        userId1 = 1L;
        userId2 = 2L;

        document1 = createDocument(userId1, "doc1.pdf", "application/pdf", DocumentType.PDF);
        document2 = createDocument(userId1, "doc2.txt", "text/plain", DocumentType.TXT);
        document3 = createDocument(userId2, "doc3.csv", "text/csv", DocumentType.CSV);

        documentRepository.saveAll(List.of(document1, document2, document3));
    }

    @Test
    @DisplayName("Should save document successfully")
    void shouldSaveDocumentSuccessfully() {
        // Given
        Document newDocument = createDocument(userId1, "new.pdf", "application/pdf", DocumentType.PDF);

        // When
        Document saved = documentRepository.save(newDocument);

        // Then
        assertNotNull(saved.getId());
        assertEquals(newDocument.getOriginalFilename(), saved.getOriginalFilename());
        assertEquals(newDocument.getUserId(), saved.getUserId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("Should find document by id")
    void shouldFindDocumentById() {
        // When
        Optional<Document> found = documentRepository.findById(document1.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(document1.getOriginalFilename(), found.get().getOriginalFilename());
        assertEquals(document1.getUserId(), found.get().getUserId());
    }

    @Test
    @DisplayName("Should return empty when document not found by id")
    void shouldReturnEmptyWhenDocumentNotFoundById() {
        // When
        Optional<Document> found = documentRepository.findById(999L);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find documents by user id")
    void shouldFindDocumentsByUserId() {
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> documents = documentRepository.findByUserId(userId1, pageable);

        // Then
        assertNotNull(documents);
        assertEquals(2, documents.getTotalElements());
        assertTrue(documents.getContent().stream()
                .allMatch(doc -> doc.getUserId().equals(userId1)));
    }

    @Test
    @DisplayName("Should find documents by user id with pagination")
    void shouldFindDocumentsByUserIdWithPagination() {
        // Given - Create more documents for user 1
        for (int i = 0; i < 25; i++) {
            Document doc = createDocument(userId1, "doc" + i + ".pdf", "application/pdf", DocumentType.PDF);
            documentRepository.save(doc);
        }

        // When - First page
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> firstPage = documentRepository.findByUserId(userId1, pageable);

        // Then
        assertEquals(10, firstPage.getContent().size());
        assertEquals(27, firstPage.getTotalElements()); // 2 from setUp + 25 new
        assertTrue(firstPage.hasNext());

        // When - Second page
        Page<Document> secondPage = documentRepository.findByUserId(userId1, pageable.next());

        // Then
        assertEquals(10, secondPage.getContent().size());
        assertTrue(secondPage.hasNext());
    }

    @Test
    @DisplayName("Should find documents by user id sorted by created date descending")
    void shouldFindDocumentsByUserIdSortedByCreatedDateDesc() {
        // When
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> documents = documentRepository.findByUserId(userId1, pageable);

        // Then
        List<Document> content = documents.getContent();
        assertEquals(2, content.size());
        // Verify descending order
        for (int i = 0; i < content.size() - 1; i++) {
            assertTrue(content.get(i).getCreatedAt().isAfter(content.get(i + 1).getCreatedAt())
                    || content.get(i).getCreatedAt().isEqual(content.get(i + 1).getCreatedAt()));
        }
    }

    @Test
    @DisplayName("Should find documents by user id and status")
    void shouldFindDocumentsByUserIdAndStatus() {
        // Given
        document1.setStatus(ProcessingStatus.COMPLETED);
        document2.setStatus(ProcessingStatus.FAILED);
        documentRepository.saveAll(List.of(document1, document2));

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> completedDocs = documentRepository
                .findByUserIdAndStatus(userId1, ProcessingStatus.COMPLETED, pageable);

        // Then
        assertNotNull(completedDocs);
        assertEquals(1, completedDocs.getTotalElements());
        assertEquals(ProcessingStatus.COMPLETED, completedDocs.getContent().getFirst().getStatus());
    }

    @Test
    @DisplayName("Should find documents by status")
    void shouldFindDocumentsByStatus() {
        // Given
        document1.setStatus(ProcessingStatus.PROCESSING);
        document2.setStatus(ProcessingStatus.PROCESSING);
        document3.setStatus(ProcessingStatus.COMPLETED);
        documentRepository.saveAll(List.of(document1, document2, document3));

        // When
        List<Document> processingDocs = documentRepository.findByStatus(ProcessingStatus.PROCESSING);

        // Then
        assertNotNull(processingDocs);
        assertEquals(2, processingDocs.size());
        assertTrue(processingDocs.stream()
                .allMatch(doc -> doc.getStatus() == ProcessingStatus.PROCESSING));
    }

    @Test
    @DisplayName("Should count documents by user id")
    void shouldCountDocumentsByUserId() {
        // When
        long count = documentRepository.countByUserId(userId1);

        // Then
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Should count documents by user id and status")
    void shouldCountDocumentsByUserIdAndStatus() {
        // Given
        document1.setStatus(ProcessingStatus.COMPLETED);
        documentRepository.save(document1);

        // When
        long count = documentRepository.countByUserIdAndStatus(userId1, ProcessingStatus.COMPLETED);

        // Then
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Should delete document")
    void shouldDeleteDocument() {
        // Given
        Long documentId = document1.getId();
        assertTrue(documentRepository.existsById(documentId));

        // When
        documentRepository.delete(document1);

        // Then
        assertFalse(documentRepository.existsById(documentId));
    }

    @Test
    @DisplayName("Should delete all documents by user id")
    void shouldDeleteAllDocumentsByUserId() {
        // When
        documentRepository.deleteByUserId(userId1);

        // Then
        Page<Document> documents = documentRepository.findByUserId(
                userId1,
                PageRequest.of(0, 10)
        );
        assertEquals(0, documents.getTotalElements());

        // User 2's documents should still exist
        Page<Document> user2Docs = documentRepository.findByUserId(
                userId2,
                PageRequest.of(0, 10)
        );
        assertEquals(1, user2Docs.getTotalElements());
    }

    @Test
    @DisplayName("Should find by stored filename")
    void shouldFindByStoredFilename() {
        // When
        Optional<Document> found = documentRepository
                .findByStoredFilename(document1.getStoredFilename());

        // Then
        assertTrue(found.isPresent());
        assertEquals(document1.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Should enforce unique constraint on stored filename")
    void shouldEnforceUniqueConstraintOnStoredFilename() {
        // Given
        Document duplicate = createDocument(userId1, "different.pdf", "application/pdf", DocumentType.PDF);
        duplicate.setStoredFilename(document1.getStoredFilename());

        // When & Then
        assertThrows(Exception.class, () -> {
            documentRepository.save(duplicate);
            documentRepository.flush();
        });
    }

    @Test
    @DisplayName("Should find documents by document type")
    void shouldFindDocumentsByDocumentType() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Document> pdfDocs = documentRepository
                .findByUserIdAndDocumentType(userId1, DocumentType.PDF, pageable);

        // Then
        assertNotNull(pdfDocs);
        assertEquals(1, pdfDocs.getTotalElements());
        assertEquals(DocumentType.PDF, pdfDocs.getContent().getFirst().getDocumentType());
    }

    @Test
    @DisplayName("Should update document status")
    void shouldUpdateDocumentStatus() {
        // Given
        document1.setStatus(ProcessingStatus.PROCESSING);
        documentRepository.save(document1);

        // When
        document1.setStatus(ProcessingStatus.COMPLETED);
        document1.setVectorStoreId("vec_123abc");
        Document updated = documentRepository.save(document1);

        // Then
        assertEquals(ProcessingStatus.COMPLETED, updated.getStatus());
        assertEquals("vec_123abc", updated.getVectorStoreId());
    }

    @Test
    @DisplayName("Should persist and retrieve audit fields")
    void shouldPersistAndRetrieveAuditFields() {
        // Given
        Document newDoc = createDocument(userId1, "audit.pdf", "application/pdf", DocumentType.PDF);

        // When
        Document saved = documentRepository.save(newDoc);

        // Then
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update updatedAt field on modification")
    void shouldUpdateUpdatedAtFieldOnModification() {
        // Given
        Document saved = documentRepository.saveAndFlush(document1);
        var originalUpdatedAt = saved.getUpdatedAt();
        Long documentId = saved.getId();

        // Clear persistence context to force fresh fetch
        entityManager.clear();

        // When
        Document toUpdate = documentRepository.findById(documentId).orElseThrow();
        toUpdate.setStatus(ProcessingStatus.COMPLETED);
        Document updated = documentRepository.saveAndFlush(toUpdate);

        // Then
        assertNotNull(updated.getUpdatedAt());
        assertTrue(updated.getUpdatedAt().compareTo(originalUpdatedAt) >= 0);
    }


    // Helper method
    private Document createDocument(Long userId, String filename, String contentType, DocumentType type) {
        Document doc = new Document();
        doc.setUserId(userId);
        doc.setOriginalFilename(filename);
        doc.setStoredFilename(UUID.randomUUID() + "-" + filename);
        doc.setContentType(contentType);
        doc.setFileSize(1024L);
        doc.setDocumentType(type);
        doc.setStatus(ProcessingStatus.UPLOADED);
        doc.setStoragePath("/storage/documents/" + userId + "/2026/01/" + filename);
        return doc;
    }
}