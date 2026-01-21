package com.marcos.usersservice.event;

import com.marcos.usersservice.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class NotificationEventPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final String topic;

    public NotificationEventPublisher(
            KafkaTemplate<String, NotificationEvent> kafkaTemplate,
            @Value("${kafka.topics.notifications}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishUserRegistered(User user) {
        var event = new NotificationEvent(
                "USER_REGISTERED",
                user.getEmail(),
                user.getUsername(),
                Map.of(
                        "userId", user.getId(),
                        "username", user.getUsername(),
                        "registrationDate", Instant.now().toString()
                )
        );
        kafkaTemplate.send(topic, user.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish USER_REGISTERED event for user {}: {}",
                                user.getId(), ex.getMessage());
                    } else {
                        log.info("Published USER_REGISTERED event for user {}", user.getId());
                    }
                });
    }
}