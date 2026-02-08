package com.marcos.leairning.ai.chat.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ConversationResponseDTO(
        UUID id,
        String title,
        Set<UUID> documentIds,
        Instant createdAt,
        Instant updatedAt
) {}
