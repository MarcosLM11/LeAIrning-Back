package com.marcos.leairning.email;

public interface EmailService {

    void sendVerificationEmail(String to, String verificationToken);

    void sendWelcomeEmail(String to, String subject);
}