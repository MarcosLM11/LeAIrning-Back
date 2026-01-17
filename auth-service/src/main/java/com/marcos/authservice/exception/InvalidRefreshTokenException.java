package com.marcos.authservice.exception;

public class InvalidRefreshTokenException extends AuthenticationException {
    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
