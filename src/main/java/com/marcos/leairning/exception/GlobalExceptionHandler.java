package com.marcos.leairning.exception;

import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;

@Flogger
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QuizzNotFoundException.class)
    public ProblemDetail handleQuizzNotFound(QuizzNotFoundException ex) {
        log.atFine().log("Quizz not found: %s", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        log.atFine().log("User not found: %s", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ProblemDetail handleDocumentNotFound(DocumentNotFoundException ex) {
        log.atFine().log("Document not found: %s", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ProblemDetail handleConversationNotFound(ConversationNotFoundException ex) {
        log.atFine().log("Conversation not found: %s", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        log.atFine().log("Email already registered: %s", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        log.atFine().log("Invalid credentials attempt");
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex) {
        log.atWarning().log("Account locked: login attempt on locked account");
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ProblemDetail handleAccountNotVerified(AccountNotVerifiedException ex) {
        log.atFine().log("Account not verified");
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ProblemDetail handleInvalidVerificationToken(InvalidVerificationTokenException ex) {
        log.atFine().log("Invalid verification token");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DocumentAccessDeniedException.class)
    public ProblemDetail handleDocumentAccessDenied(DocumentAccessDeniedException ex) {
        log.atWarning().log("Document access denied: %s", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ProblemDetail handleDocumentProcessing(DocumentProcessingException ex) {
        log.atSevere().withCause(ex).log("Document processing failed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process document");
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageError(StorageException ex) {
        log.atSevere().withCause(ex).log("Storage operation failed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Storage operation failed");
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ProblemDetail handleUnsupportedMediaType(UnsupportedMediaTypeException ex) {
        log.atFine().log("Unsupported media type: %s", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.atFine().log("Bad request: %s", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        log.atSevere().withCause(ex).log("Validation error");

        val message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");

        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);

        problem.setTitle("Validation Error");

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericError(Exception ex) {
        log.atSevere().withCause(ex).log("Unexpected error: %s", ex.getMessage());

        val problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );

        problem.setTitle("Internal Error");

        return problem;
    }
}