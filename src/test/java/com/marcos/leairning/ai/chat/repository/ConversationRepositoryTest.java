package com.marcos.leairning.ai.chat.repository;

import com.marcos.leairning.AbstractRepositoryTest;
import com.marcos.leairning.ai.chat.model.Conversation;
import com.marcos.leairning.documents.Document;
import com.marcos.leairning.documents.DocumentsRepository;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ConversationRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    ConversationRepository repository;

    @Autowired
    DocumentsRepository documentsRepository;

    UUID userId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        documentsRepository.deleteAll();
        userId = UUID.randomUUID();
    }

    @Test
    void findByUserIdOrderByLastUpdatedTimestampDesc_returnsUserConversations() {
        repository.save(createConversation(userId, "Chat 1"));
        repository.save(createConversation(userId, "Chat 2"));
        repository.save(createConversation(UUID.randomUUID(), "Other"));
        val page = repository.findByUserIdOrderByLastUpdatedTimestampDesc(userId, PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findByIdAndUserId_matchingBoth_returnsConversation() {
        val conv = createConversation(userId, "My Chat");
        repository.save(conv);
        val result = repository.findByIdAndUserId(conv.getId(), userId);
        assertTrue(result.isPresent());
        assertEquals("My Chat", result.get().getTitle());
    }

    @Test
    void findByIdAndUserId_wrongUser_returnsEmpty() {
        val conv = createConversation(userId, "My Chat");
        repository.save(conv);
        val result = repository.findByIdAndUserId(conv.getId(), UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdAndUserIdWithDocuments_loadsDocuments() {
        val doc = createDocument(userId);
        documentsRepository.save(doc);
        val conv = createConversation(userId, "With Docs");
        conv.setDocuments(Set.of(doc));
        repository.save(conv);
        val result = repository.findByIdAndUserIdWithDocuments(conv.getId(), userId);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getDocuments().size());
    }

    @Test
    void existsByIdAndUserId_existing_returnsTrue() {
        val conv = createConversation(userId, "Exists");
        repository.save(conv);
        assertTrue(repository.existsByIdAndUserId(conv.getId(), userId));
    }

    @Test
    void existsByIdAndUserId_nonExisting_returnsFalse() {
        assertFalse(repository.existsByIdAndUserId(UUID.randomUUID(), userId));
    }

    @Test
    void deleteByIdAndUserId_removesConversation() {
        val conv = createConversation(userId, "To Delete");
        repository.save(conv);
        repository.deleteByIdAndUserId(conv.getId(), userId);
        repository.flush();
        assertEquals(0, repository.count());
    }

    private Conversation createConversation(UUID ownerUserId, String title) {
        val conv = new Conversation();
        conv.setUserId(ownerUserId);
        conv.setTitle(title);
        return conv;
    }

    private Document createDocument(UUID ownerUserId) {
        val doc = new Document();
        doc.setUserId(ownerUserId);
        doc.setFileName("test.pdf");
        doc.setContentType("application/pdf");
        doc.setSize(1024L);
        doc.setStoragePath("/bucket/test.pdf");
        return doc;
    }
}
