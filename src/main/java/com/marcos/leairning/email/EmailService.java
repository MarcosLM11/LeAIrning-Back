package com.marcos.leairning.email;

import org.springframework.scheduling.annotation.Async;

public interface EmailService {

    @Async
    void sendVerificationEmail(String to, String verificationToken);

    @Async
    void sendWelcomeEmail(String to, String subject);
}