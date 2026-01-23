package com.marcos.chatservice.service;

import com.marcos.chatservice.dto.ChatResponse;

public interface ChatService {
    ChatResponse askQuestion(String question, Long userId, String conversationId);
}