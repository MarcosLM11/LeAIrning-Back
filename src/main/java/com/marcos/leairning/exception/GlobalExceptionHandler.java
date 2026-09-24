package com.marcos.leairning.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        log.warn(ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn(ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The request violates a data integrity constraint");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        problem.setTitle("Validation Error");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericError(Exception ex) {
        log.warn("Unexpected error: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Error");
        return problem;
    }
}