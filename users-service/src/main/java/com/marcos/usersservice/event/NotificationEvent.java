package com.marcos.usersservice.event;

import java.util.Map;
import java.util.Objects;

public record NotificationEvent(
        String type,
        String recipientEmail,
        String recipientName,
        Map<String, Object> payload
) {
    public NotificationEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(recipientEmail, "recipientEmail must not be null");
    }
}