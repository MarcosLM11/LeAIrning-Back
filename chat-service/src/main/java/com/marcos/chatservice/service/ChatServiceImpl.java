package com.marcos.chatservice.service;

import com.marcos.chatservice.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.time.Instant;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    public ChatServiceImpl(ChatClient chatClient, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }

    @Override
    public ChatResponse askQuestion(String question, Long userId, String conversationId) {
        log.info("Processing question for userId={}, conversationId={}", userId, conversationId);
        var userConversationId = userId + "_" + conversationId;
        var filterExpression = new FilterExpressionBuilder()
                .eq("userId", userId)
                .build();
        var searchRequest = SearchRequest.builder()
                .topK(5)
                .similarityThreshold(0.7)
                .filterExpression(filterExpression)
                .build();
        var answer = chatClient.prompt()
                .system(systemPromptTemplate)
                .user(question)
                .advisors(advisorSpec -> advisorSpec
                        .param(CONVERSATION_ID, userConversationId))
                .advisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(searchRequest)
                                .build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .call()
                .content();
        return new ChatResponse(answer, conversationId, Instant.now());
    }
}