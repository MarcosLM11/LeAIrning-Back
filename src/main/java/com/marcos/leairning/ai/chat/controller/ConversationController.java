package com.marcos.leairning.ai.chat.controller;

import com.marcos.leairning.ai.chat.dto.ChatMessageDTO;
import com.marcos.leairning.ai.chat.dto.ConversationRequestDTO;
import com.marcos.leairning.ai.chat.dto.ConversationResponseDTO;
import com.marcos.leairning.ai.chat.dto.ConversationUpdateDTO;
import com.marcos.leairning.ai.chat.service.ChatService;
import com.marcos.leairning.ai.chat.service.ConversationService;
import com.marcos.leairning.util.web.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatService chatService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponseDTO create(
            @CurrentUserId UUID userId,
            @Valid @RequestBody ConversationRequestDTO request
    ) {
        return conversationService.create(userId, request.title(), request.documentIds());
    }

    @GetMapping
    public Page<ConversationResponseDTO> list(
            @CurrentUserId UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return conversationService.findAllByUser(userId, pageable);
    }

    @GetMapping("/{id}")
    public ConversationResponseDTO getById(
            @CurrentUserId UUID userId,
            @PathVariable UUID id
    ) {
        return conversationService.findById(userId, id);
    }

    @GetMapping("/{id}/messages")
    public List<ChatMessageDTO> getMessages(
            @CurrentUserId UUID userId,
            @PathVariable UUID id
    ) {
        // First verify the conversation belongs to the user
        conversationService.findById(userId, id);
        // Then return the messages
        return chatService.getMessages(userId, id);
    }

    @PatchMapping("/{id}")
    public ConversationResponseDTO updateTitle(
            @CurrentUserId UUID userId,
            @PathVariable UUID id,
            @RequestBody ConversationUpdateDTO request
    ) {
        return conversationService.updateTitle(userId, id, request.title());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @CurrentUserId UUID userId,
            @PathVariable UUID id
    ) {
        conversationService.delete(userId, id);
    }
}
