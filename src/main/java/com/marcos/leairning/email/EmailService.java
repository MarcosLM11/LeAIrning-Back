package com.marcos.leairning.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final String FROM = "noreply@leairning.com";

    private final JavaMailSender mailSender;
    private final AuthProperties properties;
    private final TemplateService templateService;

    @Async
    public void sendVerifyEmail(String to, String token) {
        var url = properties.getFrontendUrl() + "/auth/verify?token=" + token;
        var context = Map.<String, Object>of("verificationUrl", url);
        send(to, "Verifica tu cuenta - LeAIrning", "verification-email", context, "verification");
    }

    @Async
    public void sendWelcomeEmail(String to, String subject) {
        var context = Map.<String, Object>of(
                "name", "Usuario",
                "frontendUrl", properties.getFrontendUrl()
        );
        send(to, subject, "welcome-email", context, "welcome");
    }

    private void send(String to, String subject, String template, Map<String, Object> context, String kind) {
        var htmlContent = templateService.render(template, context);
        var textContent = templateService.renderText(template, context);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);
            mailSender.send(message);
            log.info("{} email sent to {}", kind, to);
        } catch (Exception e) {
            log.error("Failed to send {} email to {}", kind, to);
        }
    }
}