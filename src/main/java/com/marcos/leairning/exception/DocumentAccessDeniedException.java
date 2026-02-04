package com.marcos.leairning.exception;

import java.util.UUID;

public class DocumentAccessDeniedException extends RuntimeException {

    public DocumentAccessDeniedException(UUID documentId, UUID userId) {
        super("User " + userId + " does not have access to document " + documentId);
    }
}