package com.marcos.leairning.ai.chat.controller;

import com.marcos.leairning.ai.chat.dto.ChatRequestDTO;
import com.marcos.leairning.ai.chat.dto.ChatResponseDTO;
import com.marcos.leairning.ai.chat.service.ChatService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatAIControllerTest {

    ChatService service;
    ChatAIController controller;

    @BeforeEach
    void setUp() {
        service = mock(ChatService.class);
        controller = new ChatAIController(service);
    }

    @Test
    void ask_returnsOkWithResponse() {
        val userId = UUID.randomUUID();
        val conversationId = UUID.randomUUID();
        val language = "es";
        val request = new ChatRequestDTO("What is AI?");
        val expected = new ChatResponseDTO("AI is...", conversationId.toString(), Instant.now());
        when(service.askQuestion(request, userId, conversationId, language)).thenReturn(expected);
        val response = controller.ask(userId, conversationId, language, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(service).askQuestion(request, userId, conversationId, language);
    }
}
