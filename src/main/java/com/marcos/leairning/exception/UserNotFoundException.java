package com.marcos.leairning.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("Unable to find user with id: " + id);
    }

    public UserNotFoundException(String email) {
        super("Unable to find user with email: " + email);
    }
}