package com.marcos.leairning.ai.chat.controller;

import com.marcos.leairning.ai.chat.dto.*;
import com.marcos.leairning.ai.chat.service.ChatService;
import com.marcos.leairning.ai.chat.service.ConversationService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversationControllerTest {

    ConversationService conversationService;
    ChatService chatService;
    ConversationController controller;

    @BeforeEach
    void setUp() {
        conversationService = mock(ConversationService.class);
        chatService = mock(ChatService.class);
        controller = new ConversationController(conversationService, chatService);
    }

    @Test
    void create_returnsConversation() {
        val userId = UUID.randomUUID();
        val docIds = Set.of(UUID.randomUUID());
        val request = new ConversationRequestDTO("My Chat", docIds);
        val expected = new ConversationResponseDTO(UUID.randomUUID(), "My Chat", docIds, Instant.now(), Instant.now());
        when(conversationService.create(userId, "My Chat", docIds)).thenReturn(expected);
        val result = controller.create(userId, request);
        assertEquals(expected, result);
    }

    @Test
    void list_returnsPage() {
        val userId = UUID.randomUUID();
        val pageable = PageRequest.of(0, 20);
        val conv = new ConversationResponseDTO(UUID.randomUUID(), "Chat", Set.of(), Instant.now(), Instant.now());
        val page = new PageImpl<>(List.of(conv));
        when(conversationService.findAllByUser(userId, pageable)).thenReturn(page);
        val result = controller.list(userId, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getById_returnsConversation() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        val expected = new ConversationResponseDTO(convId, "Chat", Set.of(), Instant.now(), Instant.now());
        when(conversationService.findById(userId, convId)).thenReturn(expected);
        val result = controller.getById(userId, convId);
        assertEquals(expected, result);
    }

    @Test
    void getMessages_verifiesOwnershipThenReturnsMessages() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        val conv = new ConversationResponseDTO(convId, "Chat", Set.of(), Instant.now(), Instant.now());
        val messages = List.of(new ChatMessageDTO("1", "user", "hello", Instant.now()));
        when(conversationService.findById(userId, convId)).thenReturn(conv);
        when(chatService.getMessages(userId, convId)).thenReturn(messages);
        val result = controller.getMessages(userId, convId);
        assertEquals(1, result.size());
        verify(conversationService).findById(userId, convId);
        verify(chatService).getMessages(userId, convId);
    }

    @Test
    void updateTitle_returnsUpdatedConversation() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        val request = new ConversationUpdateDTO("New Title");
        val expected = new ConversationResponseDTO(convId, "New Title", Set.of(), Instant.now(), Instant.now());
        when(conversationService.updateTitle(userId, convId, "New Title")).thenReturn(expected);
        val result = controller.updateTitle(userId, convId, request);
        assertEquals("New Title", result.title());
    }

    @Test
    void delete_callsService() {
        val userId = UUID.randomUUID();
        val convId = UUID.randomUUID();
        controller.delete(userId, convId);
        verify(conversationService).delete(userId, convId);
    }
}
