package com.marcos.leairning.documents;

import com.marcos.leairning.AbstractRepositoryTest;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DocumentsRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    DocumentsRepository repository;

    UUID userId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        userId = UUID.randomUUID();
    }

    @Test
    void findByUserId_returnsOnlyUserDocuments() {
        val doc1 = createDocument(userId, "file1.pdf");
        val doc2 = createDocument(userId, "file2.pdf");
        val otherDoc = createDocument(UUID.randomUUID(), "other.pdf");
        repository.saveAll(List.of(doc1, doc2, otherDoc));
        val page = repository.findByUserId(userId, PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findByIdAndUserId_matchingBoth_returnsDocument() {
        val doc = createDocument(userId, "file.pdf");
        repository.save(doc);
        val result = repository.findByIdAndUserId(doc.getId(), userId);
        assertTrue(result.isPresent());
        assertEquals("file.pdf", result.get().getFileName());
    }

    @Test
    void findByIdAndUserId_wrongUser_returnsEmpty() {
        val doc = createDocument(userId, "file.pdf");
        repository.save(doc);
        val result = repository.findByIdAndUserId(doc.getId(), UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByFileName_existingFile_returnsDocument() {
        val doc = createDocument(userId, "unique-name.pdf");
        repository.save(doc);
        val result = repository.findByFileName("unique-name.pdf");
        assertTrue(result.isPresent());
    }

    @Test
    void findByIdInAndUserId_returnsMatchingDocuments() {
        val doc1 = createDocument(userId, "file1.pdf");
        val doc2 = createDocument(userId, "file2.pdf");
        val otherDoc = createDocument(UUID.randomUUID(), "other.pdf");
        repository.saveAll(List.of(doc1, doc2, otherDoc));
        val result = repository.findByIdInAndUserId(List.of(doc1.getId(), doc2.getId(), otherDoc.getId()), userId);
        assertEquals(2, result.size());
    }

    @Test
    void deleteByIdInAndUserId_deletesOnlyMatchingDocuments() {
        val doc1 = createDocument(userId, "file1.pdf");
        val doc2 = createDocument(userId, "file2.pdf");
        val otherDoc = createDocument(UUID.randomUUID(), "other.pdf");
        repository.saveAll(List.of(doc1, doc2, otherDoc));
        val deleted = repository.deleteByIdInAndUserId(List.of(doc1.getId(), doc2.getId()), userId);
        assertEquals(2, deleted);
        assertEquals(1, repository.count());
    }

    private Document createDocument(UUID ownerUserId, String fileName) {
        val doc = new Document();
        doc.setUserId(ownerUserId);
        doc.setFileName(fileName);
        doc.setContentType("application/pdf");
        doc.setSize(1024L);
        doc.setStoragePath("/bucket/" + fileName);
        return doc;
    }
}
