package com.marcos.leairning.ai.chat.service;

import com.marcos.leairning.ai.chat.dto.ConversationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface ConversationService {

    ConversationResponseDTO create(UUID userId, String title, Set<UUID> documentIds);

    Page<ConversationResponseDTO> findAllByUser(UUID userId, Pageable pageable);

    ConversationResponseDTO findById(UUID userId, UUID conversationId);

    Set<UUID> getDocumentIds(UUID userId, UUID conversationId);

    void delete(UUID userId, UUID conversationId);

    ConversationResponseDTO updateTitle(UUID userId, UUID conversationId, String newTitle);
}
