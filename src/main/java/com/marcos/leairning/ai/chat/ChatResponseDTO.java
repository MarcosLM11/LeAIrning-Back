package com.marcos.leairning.ai.chat;

import java.time.Instant;

public record ChatResponseDTO(
        String answer,
        String conversationId,
        Instant timestamp
) {}
