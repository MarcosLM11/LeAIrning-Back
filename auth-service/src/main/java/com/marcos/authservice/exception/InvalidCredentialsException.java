package com.marcos.authservice.exception;

public class InvalidCredentialsException extends AuthenticationException {
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
