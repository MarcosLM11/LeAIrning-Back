package com.marcos.leairning.email;

import com.marcos.leairning.security.auth.AuthProperties;
import com.marcos.leairning.util.template.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final AuthProperties properties;
    private final TemplateService templateService;

    public EmailServiceImpl(JavaMailSender mailSender, AuthProperties properties, TemplateService templateService) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.templateService = templateService;
    }

    @Async
    @Override
    public void sendVerificationEmail(String to, String verificationToken) {
        var verificationUrl = properties.getFrontendUrl() + "/auth/verify?token=" + verificationToken;
        var context = Map.<String, Object>of("verificationUrl", verificationUrl);
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
            log.info("Verification email sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send verification email to {}", to);
        }
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to, String subject) {

        var context = Map.<String, Object>of(
                "name", "Usuario",
                "frontendUrl", properties.getFrontendUrl()
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
            log.info("Welcome email sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", to);
        }
    }
}