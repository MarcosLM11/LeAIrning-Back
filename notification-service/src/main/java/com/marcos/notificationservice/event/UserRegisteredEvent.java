package com.marcos.notificationservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        UUID userId,
        String email,
        String name,
        String verificationToken,
        Instant occurredAt
) {
}
