package com.marcos.leairning.ai.chat.controller;

import com.marcos.leairning.ai.chat.dto.ChatRequestDTO;
import com.marcos.leairning.ai.chat.dto.ChatResponseDTO;
import com.marcos.leairning.ai.chat.service.ChatService;
import com.marcos.leairning.util.web.CurrentUserId;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatAIController {

    private static final Logger log = LoggerFactory.getLogger(ChatAIController.class);
    private final ChatService service;

    public ChatAIController(ChatService service) {
        this.service = service;
    }

    @PostMapping(path = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponseDTO> ask(
            @CurrentUserId UUID userId,
            @RequestHeader(name = "X-Conversation-Id") UUID conversationId,
            @RequestHeader(name = "Accept-Language") String language,
            @RequestBody @Valid ChatRequestDTO request) {

        log.info("Chat request from userId={}, conversationId={}", userId, conversationId);

        var response = service.askQuestion(request, userId, conversationId, language);
        return ResponseEntity.ok(response);
    }
}
