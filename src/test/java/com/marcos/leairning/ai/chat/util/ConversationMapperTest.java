package com.marcos.leairning.ai.chat.util;

import com.marcos.leairning.ai.chat.model.Conversation;
import com.marcos.leairning.documents.Document;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ConversationMapperTest {

    ConversationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ConversationMapper();
    }

    @Test
    void toDTO_mapsAllFields() {
        val docId = UUID.randomUUID();
        val doc = new Document();
        doc.setId(docId);
        val conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setTitle("My Chat");
        conversation.setUserId(UUID.randomUUID());
        conversation.setDocuments(Set.of(doc));
        conversation.setCreatedTimestamp(Instant.parse("2026-01-01T00:00:00Z"));
        conversation.setLastUpdatedTimestamp(Instant.parse("2026-01-02T00:00:00Z"));
        val dto = mapper.toDTO(conversation);
        assertEquals(conversation.getId(), dto.id());
        assertEquals("My Chat", dto.title());
        assertEquals(Set.of(docId), dto.documentIds());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), dto.createdAt());
        assertEquals(Instant.parse("2026-01-02T00:00:00Z"), dto.updatedAt());
    }

    @Test
    void toDTO_withNoDocuments_returnsEmptySet() {
        val conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setTitle("Empty");
        conversation.setUserId(UUID.randomUUID());
        conversation.setCreatedTimestamp(Instant.now());
        conversation.setLastUpdatedTimestamp(Instant.now());
        val dto = mapper.toDTO(conversation);
        assertTrue(dto.documentIds().isEmpty());
    }
}
