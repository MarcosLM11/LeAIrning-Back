package com.marcos.documentsservice.controller;

import com.healthmarketscience.jackcess.ConstraintViolationException;
import com.marcos.documentsservice.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class DocumentControllerAdvice {
    private static final String TIMESTAMP = "timestamp";

    @ExceptionHandler(DocumentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleDocumentNotFoundException(DocumentNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Document not found");
        problem.setType(URI.create("https://api.example.com/errors/document-not-found"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access denied");
        problem.setType(URI.create("https://api.example.com/errors/access-denied"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidRequestException(InvalidRequestException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request");
        problem.setType(URI.create("https://api.example.com/errors/invalid-request"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Invalid request parameters");
        problem.setType(URI.create("https://api.example.com/errors/validation-failed"));
        problem.setProperty(TIMESTAMP, Instant.now());
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleMissingHeaderException(MissingRequestHeaderException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Missing required header: " + ex.getHeaderName());
        problem.setTitle("Authentication required");
        problem.setType(URI.create("https://api.example.com/errors/missing-header"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGeneralException(Exception ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage());
        problem.setTitle("Internal server error");
        problem.setType(URI.create("https://api.example.com/errors/internal-error"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingRequestParts(Exception ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad request");
        problem.setType(URI.create("https://api.example.com/errors/missing-parameter"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Validation failed");
        problem.setType(URI.create("https://api.example.com/errors/constraint-violation"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    @ExceptionHandler(DocumentReaderException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleDocumentReaderException(DocumentReaderException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Document reading error");
        problem.setType(URI.create("https://api.example.com/errors/document-reading-error"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    @ExceptionHandler(VectorStoreException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleVectorStoreException(VectorStoreException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Vector store error");
        problem.setType(URI.create("https://api.example.com/errors/vector-store-error"));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }
}
