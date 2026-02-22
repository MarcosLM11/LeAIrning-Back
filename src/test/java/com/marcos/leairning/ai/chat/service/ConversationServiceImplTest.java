package com.marcos.leairning.ai.chat.service;

import com.marcos.leairning.ai.chat.dto.ConversationResponseDTO;
import com.marcos.leairning.ai.chat.model.Conversation;
import com.marcos.leairning.ai.chat.repository.ConversationRepository;
import com.marcos.leairning.ai.chat.util.ConversationMapper;
import com.marcos.leairning.documents.Document;
import com.marcos.leairning.documents.DocumentsRepository;
import com.marcos.leairning.exception.ConversationNotFoundException;
import com.marcos.leairning.exception.DocumentNotFoundException;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversationServiceImplTest {

    ConversationRepository conversationRepository;
    DocumentsRepository documentsRepository;
    ConversationMapper mapper;
    ConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        documentsRepository = mock(DocumentsRepository.class);
        mapper = mock(ConversationMapper.class);
        service = new ConversationServiceImpl(conversationRepository, documentsRepository, mapper);
    }

    @Test
    void create_withValidDocuments_savesAndReturnsDTO() {
        val userId = UUID.randomUUID();
        val docId = UUID.randomUUID();
        val doc = new Document();
        doc.setId(docId);
        val saved = new Conversation();
        saved.setId(UUID.randomUUID());
        val dto = new ConversationResponseDTO(saved.getId(), "Title", Set.of(docId), Instant.now(), Instant.now());
        when(documentsRepository.findByIdInAndUserId(List.of(docId), userId)).thenReturn(List.of(doc));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);
        when(mapper.toDTO(saved)).thenReturn(dto);
        val result = service.create(userId, "Title", Set.of(docId));
        assertEquals("Title", result.title());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void create_withMissingDocuments_throwsDocumentNotFound() {
        val userId = UUID.randomUUID();
        val docId = UUID.randomUUID();
        when(documentsRepository.findByIdInAndUserId(List.of(docId), userId)).thenReturn(List.of());
        assertThrows(DocumentNotFoundException.class, () -> service.create(userId, "Title", Set.of(docId)));
    }

    @Test
    void findAllByUser_returnsPage() {
        val userId = UUID.randomUUID();
        val pageable = PageRequest.of(0, 20);
        val conv = new Conversation();
        val dto = new ConversationResponseDTO(UUID.randomUUID(), "T", Set.of(), Instant.now(), Instant.now());
        when(conversationRepository.findByUserIdOrderByLastUpdatedTimestampDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(conv)));
        when(mapper.toDTO(conv)).thenReturn(dto);
        val result = service.findAllByUser(userId, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findById_existing_returnsDTO() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        val conv = new Conversation();
        val dto = new ConversationResponseDTO(convId, "T", Set.of(), Instant.now(), Instant.now());
        when(conversationRepository.findByIdAndUserIdWithDocuments(convId, userId)).thenReturn(Optional.of(conv));
        when(mapper.toDTO(conv)).thenReturn(dto);
        val result = service.findById(userId, convId);
        assertEquals(convId, result.id());
    }

    @Test
    void findById_nonExisting_throwsConversationNotFound() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserIdWithDocuments(convId, userId)).thenReturn(Optional.empty());
        assertThrows(ConversationNotFoundException.class, () -> service.findById(userId, convId));
    }

    @Test
    void getDocumentIds_existing_returnsIds() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        val docId = UUID.randomUUID();
        val doc = new Document();
        doc.setId(docId);
        val conv = new Conversation();
        conv.setDocuments(Set.of(doc));
        when(conversationRepository.findByIdAndUserIdWithDocuments(convId, userId)).thenReturn(Optional.of(conv));
        val result = service.getDocumentIds(userId, convId);
        assertEquals(Set.of(docId), result);
    }

    @Test
    void getDocumentIds_nonExisting_throwsConversationNotFound() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserIdWithDocuments(convId, userId)).thenReturn(Optional.empty());
        assertThrows(ConversationNotFoundException.class, () -> service.getDocumentIds(userId, convId));
    }

    @Test
    void delete_existing_deletesConversation() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        when(conversationRepository.existsByIdAndUserId(convId, userId)).thenReturn(true);
        service.delete(userId, convId);
        verify(conversationRepository).deleteByIdAndUserId(convId, userId);
    }

    @Test
    void delete_nonExisting_throwsConversationNotFound() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        when(conversationRepository.existsByIdAndUserId(convId, userId)).thenReturn(false);
        assertThrows(ConversationNotFoundException.class, () -> service.delete(userId, convId));
    }

    @Test
    void updateTitle_existing_updatesAndReturnsDTO() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        val conv = new Conversation();
        conv.setTitle("Old");
        val saved = new Conversation();
        saved.setTitle("New");
        val dto = new ConversationResponseDTO(convId, "New", Set.of(), Instant.now(), Instant.now());
        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(conv));
        when(conversationRepository.save(conv)).thenReturn(saved);
        when(mapper.toDTO(saved)).thenReturn(dto);
        val result = service.updateTitle(userId, convId, "New");
        assertEquals("New", result.title());
        assertEquals("New", conv.getTitle());
    }

    @Test
    void updateTitle_nonExisting_throwsConversationNotFound() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.empty());
        assertThrows(ConversationNotFoundException.class, () -> service.updateTitle(userId, convId, "New"));
    }
}
