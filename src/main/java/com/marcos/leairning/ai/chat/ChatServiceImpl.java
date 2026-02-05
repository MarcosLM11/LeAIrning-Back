package com.marcos.leairning.ai.chat;

import com.marcos.leairning.ai.conversation.ConversationService;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Flogger
@Service
public class ChatServiceImpl implements ChatService {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    private final ChatClient client;
    private final ChatMemory memory;
    private final VectorStore vectorStore;
    private final ConversationService conversationService;

    public ChatServiceImpl(
            ChatClient client,
            ChatMemory memory,
            VectorStore vectorStore,
            ConversationService conversationService
    ) {
        this.client = client;
        this.memory = memory;
        this.vectorStore = vectorStore;
        this.conversationService = conversationService;
    }

    @Override
    public ChatResponseDTO askQuestion(String question, UUID userId, String conversationId) {
        log.atInfo().log("Processing question for userId=%s, conversationId=%s", userId, conversationId);

        val userConversationId = userId + "_" + conversationId;

        // Get document IDs from the conversation
        Set<UUID> documentIds;
        try {
            documentIds = conversationService.getDocumentIds(userId, UUID.fromString(conversationId));
        } catch (IllegalArgumentException e) {
            // If conversationId is not a valid UUID (e.g., "default"), fall back to all user documents
            log.atWarning().log("Invalid conversationId format, falling back to all user documents");
            documentIds = Set.of();
        }

        val filterExpression = buildFilterExpression(userId, documentIds);

        val searchRequest = SearchRequest.builder()
                .topK(5)
                .similarityThreshold(0.7)
                .filterExpression(filterExpression)
                .build();

        val answer = client.prompt()
                .system(systemPromptTemplate)
                .user(question)
                .advisors(advisorSpec -> advisorSpec
                        .param(CONVERSATION_ID, userConversationId))
                .advisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(searchRequest)
                                .build(),
                        MessageChatMemoryAdvisor.builder(memory).build())
                .call()
                .content();

        return new ChatResponseDTO(answer, conversationId, Instant.now());
    }

    private Filter.Expression buildFilterExpression(UUID userId, Set<UUID> documentIds) {
        val builder = new FilterExpressionBuilder();

        if (documentIds == null || documentIds.isEmpty()) {
            // No specific documents selected, search all user's documents
            return builder.eq("userId", userId.toString()).build();
        }

        // Filter by userId AND documentId IN (...)
        val documentIdStrings = documentIds.stream()
                .map(UUID::toString)
                .toList();

        return builder.and(
                builder.eq("userId", userId.toString()),
                builder.in("documentId", documentIdStrings.toArray(new String[0]))
        ).build();
    }
}
