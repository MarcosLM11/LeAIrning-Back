package com.marcos.leairning.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/chats/{userId}")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<String> chat(@PathVariable UUID userId, @RequestBody String message) {
        var response = service.chat(userId,message);
        return ResponseEntity.ok(response);
    }
}
