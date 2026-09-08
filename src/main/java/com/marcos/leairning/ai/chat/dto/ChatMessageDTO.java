package com.marcos.leairning.ai.chat.dto;

import java.time.Instant;

public record ChatMessageDTO(
        String id,
        String role,
        String content,
        Instant timestamp
) {}
