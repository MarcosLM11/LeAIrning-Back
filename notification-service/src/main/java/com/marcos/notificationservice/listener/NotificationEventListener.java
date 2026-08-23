package com.marcos.notificationservice.listener;

import com.marcos.notificationservice.event.UserRegisteredEvent;
import com.marcos.notificationservice.event.UserVerifiedEvent;
import com.marcos.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;

    @KafkaListener(topics = "user-registered", groupId = "notification-service")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for {}", event.email());
        emailService.sendVerificationEmail(event.email(), event.name(), event.verificationToken());
    }

    @KafkaListener(topics = "user-verified", groupId = "notification-service")
    public void onUserVerified(UserVerifiedEvent event) {
        log.info("Received UserVerifiedEvent for {}", event.email());
        emailService.sendWelcomeEmail(event.email(), "¡Bienvenido a LeAIrning!");
    }
}
