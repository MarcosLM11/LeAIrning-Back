package com.marcos.notificationservice.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class EmailServiceImpl implements EmailService {

    private static final String frontendUrl = "http://localhost:4200";

    private final JavaMailSender mailSender;
    private final TemplateService templateService;

    private final Logger log = Logger.getLogger(EmailServiceImpl.class.getName());

    public EmailServiceImpl(JavaMailSender mailSender, TemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }


    @Async
    @Override
    public void sendVerificationEmail(String to, String name, String verificationToken) {
        var verificationUrl = frontendUrl + "/auth/verify?token=" + verificationToken;

        var context = Map.<String, Object>of(
                "name", name,
                "verificationUrl", verificationUrl
        );

        var htmlContent = templateService.render("verification-email", context);
        var textContent = templateService.renderText("verification-email", context);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@leairning.com");
            helper.setTo(to);
            helper.setSubject("Verifica tu cuenta - LeAIrning");
            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Verification email sent to " + to);
        } catch (Exception e) {
            log.severe("Failed to send verification email to " + to);
            log.severe(e.toString());
        }
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to, String name, String subject) {

        var context = Map.<String, Object>of(
                "name", name,
                "frontendUrl", frontendUrl
        );

        var htmlContent = templateService.render("welcome-email", context);
        var textContent = templateService.renderText("welcome-email", context);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@leairning.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Welcome email sent to " + to);

        } catch (Exception e) {
            log.severe("Failed to send welcome email to " + to);
            log.severe(e.toString());
        }
    }
}