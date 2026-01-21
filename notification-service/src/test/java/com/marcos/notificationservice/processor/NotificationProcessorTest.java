package com.marcos.notificationservice.processor;

import com.marcos.notificationservice.dto.NotificationEvent;
import com.marcos.notificationservice.handler.NotificationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock
    private NotificationHandler welcomeHandler;

    @Mock
    private NotificationHandler passwordResetHandler;

    private NotificationProcessor processor;

    @BeforeEach
    void setUp() {
        when(welcomeHandler.getType()).thenReturn("USER_REGISTERED");
        when(passwordResetHandler.getType()).thenReturn("PASSWORD_RESET");
        processor = new NotificationProcessor(List.of(welcomeHandler, passwordResetHandler));
    }

    @Test
    void shouldRouteEventToCorrectHandler() {
        var event = new NotificationEvent("USER_REGISTERED", "test@example.com", "Test User", Map.of());
        processor.process(event);
        verify(welcomeHandler).handle(event);
        verify(passwordResetHandler, never()).handle(any());
    }

    @Test
    void shouldRouteToPasswordResetHandler() {
        var event = new NotificationEvent("PASSWORD_RESET", "test@example.com", "Test User", Map.of());
        processor.process(event);
        verify(passwordResetHandler).handle(event);
        verify(welcomeHandler, never()).handle(any());
    }

    @Test
    void shouldNotFailForUnknownEventType() {
        var event = new NotificationEvent("UNKNOWN_TYPE", "test@example.com", "Test User", Map.of());
        processor.process(event);
        verify(welcomeHandler, never()).handle(any());
        verify(passwordResetHandler, never()).handle(any());
    }

    @Test
    void shouldWorkWithEmptyHandlerList() {
        var emptyProcessor = new NotificationProcessor(List.of());
        var event = new NotificationEvent("USER_REGISTERED", "test@example.com", "Test User", Map.of());
        emptyProcessor.process(event);
    }
}