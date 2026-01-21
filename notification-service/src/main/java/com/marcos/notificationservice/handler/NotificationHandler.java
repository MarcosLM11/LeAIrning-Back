package com.marcos.notificationservice.handler;

import com.marcos.notificationservice.dto.NotificationEvent;

public interface NotificationHandler {
    String getType();
    void handle(NotificationEvent event);
}