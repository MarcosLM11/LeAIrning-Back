package com.marcos.notificationservice.handler;

import com.marcos.notificationservice.dto.NotificationEvent;
import com.marcos.notificationservice.service.EmailService;
import com.marcos.notificationservice.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeEmailHandler implements NotificationHandler {

    private static final String TYPE = "USER_REGISTERED";
    private static final String SUBJECT = "Bienvenido a LeAIrning";

    private final EmailService emailService;
    private final TemplateService templateService;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void handle(NotificationEvent event) {
        log.debug("Processing welcome email for: {}", event.recipientEmail());
        var context = Map.<String, Object>of(
                "name", event.recipientName(),
                "username", event.payload().get("username")
        );
        var htmlContent = templateService.render("welcome-email", context);
        var textContent = templateService.renderText("welcome-email", context);
        emailService.sendMultipartEmail(event.recipientEmail(), SUBJECT, htmlContent, textContent);
    }
}