package com.marcos.chatservice.controller;

import com.marcos.chatservice.dto.ChatRequest;
import com.marcos.chatservice.dto.ChatResponse;
import com.marcos.chatservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{version}/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping(path = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> ask(
            @PathVariable String version,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(name = "X-Conversation-Id", defaultValue = "default") String conversationId,
            @RequestBody @Valid ChatRequest request) {
        log.info("Chat request from userId={}, conversationId={}", userId, conversationId);
        var response = chatService.askQuestion(request.question(), userId, conversationId);
        return ResponseEntity.ok(response);
    }
}