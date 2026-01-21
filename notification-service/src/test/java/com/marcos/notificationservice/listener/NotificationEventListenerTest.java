package com.marcos.notificationservice.listener;

import com.marcos.notificationservice.dto.NotificationEvent;
import com.marcos.notificationservice.processor.NotificationProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationProcessor processor;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void shouldDelegateEventToProcessor() {
        var event = new NotificationEvent("USER_REGISTERED", "test@example.com", "Test User", Map.of());
        listener.onNotificationEvent(event);
        verify(processor).process(event);
    }

    @Test
    void shouldNotPropagateExceptionFromProcessor() {
        var event = new NotificationEvent("USER_REGISTERED", "test@example.com", "Test User", Map.of());
        doThrow(new RuntimeException("Processing error")).when(processor).process(event);
        listener.onNotificationEvent(event);
        verify(processor).process(event);
    }
}