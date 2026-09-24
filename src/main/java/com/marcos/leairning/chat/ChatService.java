package com.marcos.leairning.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(UUID userId, String message) {
        var response = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, userId.toString()))
                .call().chatResponse();
        if (response == null) return "no content";
        var tokens = response.getMetadata().getUsage();
        log.info("(Token usage: {} input, output: {}, total: {})",tokens.getPromptTokens(), tokens.getCompletionTokens(), tokens.getTotalTokens());
        return response.getResult().getOutput().getText();
    }
}