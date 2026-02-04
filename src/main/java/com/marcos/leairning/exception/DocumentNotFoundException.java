package com.marcos.leairning.exception;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID id) {
        super("Unable to find document with id: " + id);
    }
}