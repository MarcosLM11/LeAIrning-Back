package com.marcos.notificationservice.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @Test
    void shouldSendMultipartEmail() throws Exception {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@leairning.com");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        emailService.sendMultipartEmail("test@example.com", "Subject", "<html>HTML</html>", "Plain text");
        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue()).isEqualTo(mimeMessage);
    }

    @Test
    void shouldNotThrowWhenSendingFails() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@leairning.com");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));
        emailService.sendMultipartEmail("test@example.com", "Subject", "<html>HTML</html>", "Plain text");
        verify(mailSender).send(any(MimeMessage.class));
    }
}