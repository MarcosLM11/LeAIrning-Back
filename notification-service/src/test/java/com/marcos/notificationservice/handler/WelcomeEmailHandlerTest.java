package com.marcos.notificationservice.handler;

import com.marcos.notificationservice.dto.NotificationEvent;
import com.marcos.notificationservice.service.EmailService;
import com.marcos.notificationservice.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WelcomeEmailHandlerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private TemplateService templateService;

    private WelcomeEmailHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WelcomeEmailHandler(emailService, templateService);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(handler.getType()).isEqualTo("USER_REGISTERED");
    }

    @Test
    void shouldSendWelcomeEmail() {
        var event = new NotificationEvent(
                "USER_REGISTERED",
                "john@example.com",
                "John Doe",
                Map.of("username", "john.doe")
        );
        when(templateService.render(eq("welcome-email"), any())).thenReturn("<html>Welcome</html>");
        when(templateService.renderText(eq("welcome-email"), any())).thenReturn("Welcome text");
        handler.handle(event);
        verify(emailService).sendMultipartEmail(
                eq("john@example.com"),
                eq("Bienvenido a LeAIrning"),
                eq("<html>Welcome</html>"),
                eq("Welcome text")
        );
    }

    @Test
    void shouldPassCorrectVariablesToTemplate() {
        var event = new NotificationEvent(
                "USER_REGISTERED",
                "john@example.com",
                "John Doe",
                Map.of("username", "john.doe")
        );
        when(templateService.render(eq("welcome-email"), any())).thenReturn("<html>Welcome</html>");
        when(templateService.renderText(eq("welcome-email"), any())).thenReturn("Welcome text");
        handler.handle(event);
        verify(templateService).render(eq("welcome-email"), argThat(vars ->
                vars.get("name").equals("John Doe") && vars.get("username").equals("john.doe")
        ));
    }
}