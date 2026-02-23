package com.marcos.leairning.ai.chat.controller;

import com.marcos.leairning.ai.chat.dto.ChatRequestDTO;
import com.marcos.leairning.ai.chat.dto.ChatResponseDTO;
import com.marcos.leairning.ai.chat.service.ChatService;
import com.marcos.leairning.util.web.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Flogger
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ChatAIController {

    ChatService service;

    @PostMapping(path = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponseDTO> ask(
            @CurrentUserId UUID userId,
            @RequestHeader(name = "X-Conversation-Id") UUID conversationId,
            @RequestHeader(name = "Accept-Language") String language,
            @RequestBody @Valid ChatRequestDTO request) {

        log.atInfo().log("Chat request from userId=%s, conversationId=%s", userId, conversationId);

        val response = service.askQuestion(request, userId, conversationId, language);
        return ResponseEntity.ok(response);
    }
}
