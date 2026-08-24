package com.marcos.notificationservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserVerifiedEvent(
        UUID eventId,
        UUID userId,
        String email,
        String name,
        Instant occurredAt
) {
}
