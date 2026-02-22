package com.marcos.leairning.email;

import com.marcos.leairning.security.auth.AuthProperties;
import com.marcos.leairning.util.template.TemplateService;
import jakarta.mail.internet.MimeMessage;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    JavaMailSender mailSender;
    AuthProperties properties;
    TemplateService templateService;
    EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        properties = mock(AuthProperties.class);
        templateService = mock(TemplateService.class);
        service = new EmailServiceImpl(mailSender, properties, templateService);
    }

    @Test
    void sendVerificationEmail_sendsEmail() {
        val mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(properties.getFrontendUrl()).thenReturn("http://localhost:4200");
        when(templateService.render(eq("verification-email"), anyMap())).thenReturn("<html>verify</html>");
        when(templateService.renderText(eq("verification-email"), anyMap())).thenReturn("verify");
        service.sendVerificationEmail("test@test.com", "token123");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendVerificationEmail_onException_doesNotThrow() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("mail error"));
        when(properties.getFrontendUrl()).thenReturn("http://localhost:4200");
        when(templateService.render(eq("verification-email"), anyMap())).thenReturn("<html>");
        when(templateService.renderText(eq("verification-email"), anyMap())).thenReturn("text");
        assertDoesNotThrow(() -> service.sendVerificationEmail("test@test.com", "token"));
    }

    @Test
    void sendWelcomeEmail_sendsEmail() {
        val mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(properties.getFrontendUrl()).thenReturn("http://localhost:4200");
        when(templateService.render(eq("welcome-email"), anyMap())).thenReturn("<html>welcome</html>");
        when(templateService.renderText(eq("welcome-email"), anyMap())).thenReturn("welcome");
        service.sendWelcomeEmail("test@test.com", "Welcome!");
        verify(mailSender).send(mimeMessage);
    }

    private static void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new AssertionError("Expected no exception, but got: " + e.getMessage(), e);
        }
    }
}
