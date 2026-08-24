package com.marcos.notificationservice.service;

public interface EmailService {
    void sendVerificationEmail(String to, String name, String verificationToken);
    void sendWelcomeEmail(String to, String name, String subject);
}