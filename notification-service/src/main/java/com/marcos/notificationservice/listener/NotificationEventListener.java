package com.marcos.notificationservice.listener;

import com.marcos.notificationservice.dto.NotificationEvent;
import com.marcos.notificationservice.processor.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationProcessor processor;

    @KafkaListener(topics = "${kafka.topics.notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void onNotificationEvent(NotificationEvent event) {
        log.info("Received notification event: type={}, recipient={}", event.type(), event.recipientEmail());
        try {
            processor.process(event);
        } catch (Exception e) {
            log.error("Failed to process notification event: type={}, recipient={}, error={}", event.type(), event.recipientEmail(), e.getMessage());
        }
    }
}