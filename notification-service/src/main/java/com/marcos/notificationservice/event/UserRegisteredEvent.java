package com.marcos.notificationservice.event;

public record UserRegisteredEvent(String email, String verificationToken) {
}
