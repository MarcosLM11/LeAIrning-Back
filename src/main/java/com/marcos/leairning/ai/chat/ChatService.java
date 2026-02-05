package com.marcos.leairning.ai.chat;

import java.util.UUID;

public interface ChatService {

    ChatResponseDTO askQuestion(String question, UUID userId, String conversationId);
}
