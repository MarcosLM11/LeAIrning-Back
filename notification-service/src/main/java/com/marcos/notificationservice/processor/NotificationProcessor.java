package com.marcos.notificationservice.processor;

import com.marcos.notificationservice.dto.NotificationEvent;
import com.marcos.notificationservice.handler.NotificationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotificationProcessor {

    private final Map<String, NotificationHandler> handlers;

    public NotificationProcessor(List<NotificationHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(NotificationHandler::getType, Function.identity()));
        log.info("Registered notification handlers: {}", handlers.keySet());
    }

    public void process(NotificationEvent event) {
        var handler = handlers.get(event.type());
        if (handler == null) {
            log.warn("No handler found for notification type: {}", event.type());
            return;
        }
        handler.handle(event);
    }
}