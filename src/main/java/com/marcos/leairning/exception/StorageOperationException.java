package com.marcos.leairning.exception;

public class StorageOperationException extends StorageException {

    public StorageOperationException(String operation, Throwable cause) {
        super("Failed to " + operation, cause);
    }
}