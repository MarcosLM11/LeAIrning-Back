package com.marcos.chatservice.dto;

import java.time.Instant;

public record ChatResponse(
        String answer,
        String conversationId,
        Instant timestamp
) {}