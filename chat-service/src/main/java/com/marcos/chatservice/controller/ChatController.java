package com.marcos.chatservice.controller;

import com.marcos.chatservice.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(path="/ask", produces = "application/json")
    public String ask(@RequestHeader(name="X_AI_CONVERSATION_ID", defaultValue = "default") String conversationID,
                      @RequestBody String question) {
        return chatService.askQuestion(question, conversationID);
    }

}
