package com.marcos.leairning.ai.chat;

import java.time.Instant;

/**
 * DTO representing a chat message in a conversation.
 *
 * @param id        unique identifier of the message
 * @param role      the role of the message sender (user or assistant)
 * @param content   the message content
 * @param timestamp when the message was created
 */
public record ChatMessageDTO(
        String id,
        String role,
        String content,
        Instant timestamp
) {}
