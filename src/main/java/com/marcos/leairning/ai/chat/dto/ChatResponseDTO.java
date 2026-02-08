package com.marcos.leairning.ai.chat.dto;

import java.time.Instant;

public record ChatResponseDTO(
        String answer,
        String conversationId,
        Instant timestamp
) {}