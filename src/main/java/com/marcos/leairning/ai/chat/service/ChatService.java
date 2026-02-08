package com.marcos.leairning.ai.chat.service;

import com.marcos.leairning.ai.chat.dto.ChatMessageDTO;
import com.marcos.leairning.ai.chat.dto.ChatRequestDTO;
import com.marcos.leairning.ai.chat.dto.ChatResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatResponseDTO askQuestion(ChatRequestDTO request, UUID userId, UUID conversationId);

    List<ChatMessageDTO> getMessages(UUID userId, UUID conversationId);
}
