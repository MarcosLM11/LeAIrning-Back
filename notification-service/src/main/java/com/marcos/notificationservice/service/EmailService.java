package com.marcos.notificationservice.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    @Async("notificationExecutor")
    public void sendMultipartEmail(String to, String subject, String htmlContent, String textContent) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException | RuntimeException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}