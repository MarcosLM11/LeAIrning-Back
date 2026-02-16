package com.marcos.leairning.ai.chat.service.ollama;

import com.marcos.leairning.ai.chat.dto.ChatRequestDTO;
import com.marcos.leairning.ai.chat.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import java.lang.reflect.Field;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceOllamaImplTest {

    ChatMemory chatMemory;
    ConversationService conversationService;
    ChatClient.Builder chatClientBuilder;
    ChatClient chatClient;
    VectorStore vectorStore;
    ChatServiceOllamaImpl chatService;
    Resource systemPromptTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        chatMemory = mock(ChatMemory.class);
        conversationService = mock(ConversationService.class);
        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        vectorStore = mock(VectorStore.class);
        systemPromptTemplate = mock(Resource.class);

        // Setup ChatClient builder chain with proper return values
        ChatClient.Builder clonedBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.clone()).thenReturn(clonedBuilder);
        when(clonedBuilder.defaultSystem(any(Resource.class))).thenReturn(clonedBuilder);
        when(clonedBuilder.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.api.Advisor[].class))).thenReturn(clonedBuilder);
        when(clonedBuilder.build()).thenReturn(chatClient);

        // Setup request chain
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.ChatClientRequestSpec userRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.ChatClientRequestSpec advisorRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(userRequestSpec);
        when(userRequestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(advisorRequestSpec);
        when(advisorRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Test response from AI");

        chatService = new ChatServiceOllamaImpl(chatMemory, vectorStore, chatClientBuilder, conversationService);
        // Use reflection to set the private field
        try {
            Field field = ChatServiceOllamaImpl.class.getDeclaredField("systemPromptTemplate");
            field.setAccessible(true);
            field.set(chatService, systemPromptTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set systemPromptTemplate", e);
        }
    }

    @Test
    void askQuestion_shouldSaveUserMessageToChatMemory() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        String compositeId = userId + "_" + conversationId;
        ChatRequestDTO request = new ChatRequestDTO("Hello AI, how are you?");

        when(conversationService.getDocumentIds(userId, conversationId)).thenReturn(java.util.Set.of());

        chatService.askQuestion(request, userId, conversationId);

        // Verify user message was saved to chat memory (at least once)
        verify(chatMemory, atLeastOnce()).add(eq(compositeId), any(Message.class));
    }

    @Test
    void askQuestion_shouldSaveBothMessagesInChatMemory() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        String compositeId = userId + "_" + conversationId;
        ChatRequestDTO request = new ChatRequestDTO("Test question");

        when(conversationService.getDocumentIds(userId, conversationId)).thenReturn(java.util.Set.of());

        chatService.askQuestion(request, userId, conversationId);

        // Verify both messages were saved (user + assistant)
        verify(chatMemory, times(2)).add(eq(compositeId), any(Message.class));
    }

    @Test
    void askQuestion_shouldReturnResponseWithCorrectData() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        ChatRequestDTO request = new ChatRequestDTO("Hello");

        when(conversationService.getDocumentIds(userId, conversationId)).thenReturn(java.util.Set.of());

        var response = chatService.askQuestion(request, userId, conversationId);

        assertNotNull(response);
        assertEquals("Test response from AI", response.answer());
        assertEquals(conversationId.toString(), response.conversationId());
        assertNotNull(response.timestamp());
    }
}
