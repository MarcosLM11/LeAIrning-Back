package com.marcos.usersservice.event;

import com.marcos.usersservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Mock
    private SendResult<String, NotificationEvent> sendResult;

    private NotificationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new NotificationEventPublisher(kafkaTemplate, "notifications");
    }

    @Test
    void shouldPublishUserRegisteredEvent() {
        var user = createTestUser();
        CompletableFuture<SendResult<String, NotificationEvent>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("notifications"), eq("1"), any(NotificationEvent.class))).thenReturn(future);
        publisher.publishUserRegistered(user);
        var eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaTemplate).send(eq("notifications"), eq("1"), eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo("USER_REGISTERED");
        assertThat(event.recipientEmail()).isEqualTo("john@example.com");
        assertThat(event.recipientName()).isEqualTo("john.doe");
        assertThat(event.payload()).containsEntry("userId", 1L);
        assertThat(event.payload()).containsEntry("username", "john.doe");
    }

    @Test
    void shouldNotThrowWhenPublishingFails() {
        var user = createTestUser();
        CompletableFuture<SendResult<String, NotificationEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(eq("notifications"), eq("1"), any(NotificationEvent.class))).thenReturn(future);
        publisher.publishUserRegistered(user);
        verify(kafkaTemplate).send(eq("notifications"), eq("1"), any(NotificationEvent.class));
    }

    private User createTestUser() {
        var user = new User();
        user.setId(1L);
        user.setUsername("john.doe");
        user.setEmail("john@example.com");
        return user;
    }
}