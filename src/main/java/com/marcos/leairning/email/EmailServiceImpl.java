package com.marcos.leairning.email;

import com.marcos.leairning.security.auth.AuthProperties;
import com.marcos.leairning.util.template.TemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;

@Flogger
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailServiceImpl implements EmailService {

    JavaMailSender mailSender;
    AuthProperties properties;
    TemplateService templateService;


    @Async
    @Override
    public void sendVerificationEmail(String to, String verificationToken) {
        val verificationUrl = properties.getFrontendUrl() + "/auth/verify?token=" + verificationToken;
        val message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Verify your LeAIrning account");
        message.setText("Click the following link to verify your account:\n\n" + verificationUrl);

        mailSender.send(message);
        log.atInfo().log("Verification email sent to {}", to);
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to, String subject) {

        var context = Map.<String, Object>of(
                "name", "User"
        );

        val htmlContent = templateService.render("welcome-email", context);
        val textContent = templateService.renderText("welcome-email", context);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@leairning.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(message);
                log.atInfo().log("Welcome email sent to {}", to);

        } catch (Exception e) {
            log.atSevere().withCause(e).log("Failed to send welcome email to %s", to);
        }
    }
}