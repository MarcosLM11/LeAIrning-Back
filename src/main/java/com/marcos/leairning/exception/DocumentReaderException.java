package com.marcos.leairning.exception;

public class DocumentReaderException extends RuntimeException
{
    public DocumentReaderException(String message) {
        super(message);
    }

    public DocumentReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
