package com.marcos.notificationservice.dto;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationEventTest {

    @Test
    void shouldCreateNotificationEventWithAllFields() {
        var payload = Map.<String, Object>of("userId", 123L, "username", "john.doe");
        var event = new NotificationEvent("USER_REGISTERED", "test@example.com", "John Doe", payload);
        assertThat(event.type()).isEqualTo("USER_REGISTERED");
        assertThat(event.recipientEmail()).isEqualTo("test@example.com");
        assertThat(event.recipientName()).isEqualTo("John Doe");
        assertThat(event.payload()).containsEntry("userId", 123L);
    }

    @Test
    void shouldRejectNullType() {
        assertThatThrownBy(() -> new NotificationEvent(null, "test@example.com", "John", Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
    }

    @Test
    void shouldRejectNullRecipientEmail() {
        assertThatThrownBy(() -> new NotificationEvent("USER_REGISTERED", null, "John", Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("recipientEmail");
    }

    @Test
    void shouldAllowNullRecipientNameAndPayload() {
        var event = new NotificationEvent("USER_REGISTERED", "test@example.com", null, null);
        assertThat(event.recipientName()).isNull();
        assertThat(event.payload()).isNull();
    }
}