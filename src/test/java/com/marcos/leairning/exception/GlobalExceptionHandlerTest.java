package com.marcos.leairning.exception;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleQuizzNotFound_returns404() {
        val result = handler.handleQuizzNotFound(new QuizzNotFoundException("not found"));
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
    }

    @Test
    void handleUserNotFound_returns404() {
        val result = handler.handleUserNotFound(new UserNotFoundException(UUID.randomUUID()));
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
    }

    @Test
    void handleDocumentNotFound_returns404() {
        val result = handler.handleDocumentNotFound(new DocumentNotFoundException(UUID.randomUUID()));
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
    }

    @Test
    void handleConversationNotFound_returns404() {
        val result = handler.handleConversationNotFound(new ConversationNotFoundException("not found"));
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
    }

    @Test
    void handleEmailAlreadyRegistered_returns409() {
        val result = handler.handleEmailAlreadyRegistered(new EmailAlreadyRegisteredException("test@test.com"));
        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
    }

    @Test
    void handleInvalidCredentials_returns401() {
        val result = handler.handleInvalidCredentials(new InvalidCredentialsException());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getStatus());
    }

    @Test
    void handleAccountLocked_returns429() {
        val result = handler.handleAccountLocked(new AccountLockedException());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), result.getStatus());
    }

    @Test
    void handleAccountNotVerified_returns403() {
        val result = handler.handleAccountNotVerified(new AccountNotVerifiedException());
        assertEquals(HttpStatus.FORBIDDEN.value(), result.getStatus());
    }

    @Test
    void handleInvalidVerificationToken_returns400() {
        val result = handler.handleInvalidVerificationToken(new InvalidVerificationTokenException());
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
    }

    @Test
    void handleDocumentAccessDenied_returns403() {
        val result = handler.handleDocumentAccessDenied(
                new DocumentAccessDeniedException(UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(HttpStatus.FORBIDDEN.value(), result.getStatus());
    }

    @Test
    void handleDocumentProcessing_returns500() {
        val result = handler.handleDocumentProcessing(
                new DocumentProcessingException("failed", new RuntimeException()));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
    }

    @Test
    void handleStorageError_returns500() {
        val result = handler.handleStorageError(new StorageException("failed"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
    }

    @Test
    void handleUnsupportedMediaType_returns415() {
        val result = handler.handleUnsupportedMediaType(
                new UnsupportedMediaTypeException("unsupported"));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), result.getStatus());
    }

    @Test
    void handleIllegalArgument_returns400() {
        val result = handler.handleIllegalArgument(new IllegalArgumentException("bad arg"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
    }

    @Test
    void handleValidation_returns400WithFieldErrors() {
        val bindingResult = mock(BindingResult.class);
        val fieldError = new FieldError("obj", "email", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        val ex = new MethodArgumentNotValidException(null, bindingResult);
        val result = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("Validation Error", result.getTitle());
        assertTrue(result.getDetail().contains("email"));
    }

    @Test
    void handleGenericError_returns500() {
        val result = handler.handleGenericError(new RuntimeException("unexpected"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
        assertEquals("Internal Error", result.getTitle());
    }
}
