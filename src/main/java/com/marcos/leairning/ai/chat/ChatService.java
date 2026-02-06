package com.marcos.leairning.ai.chat;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatResponseDTO askQuestion(String question, UUID userId, String conversationId);

    List<ChatMessageDTO> getMessages(UUID userId, UUID conversationId);
}
