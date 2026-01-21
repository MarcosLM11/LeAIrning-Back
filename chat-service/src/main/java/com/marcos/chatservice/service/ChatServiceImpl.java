package com.marcos.chatservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private String systemPromptTemplate;

    public ChatServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String askQuestion(String question, String conversationID) {

        return chatClient.prompt()
                .system(systemSpec -> systemSpec
                        .text(systemPromptTemplate))
                .user(question)
                .advisors(advisorSpec -> advisorSpec
                        .param(CONVERSATION_ID, conversationID))
                .call()
                .content();
    }
}

