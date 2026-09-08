package com.marcos.leairning.ai.chat.service.impl;

import com.marcos.leairning.ai.chat.dto.ChatMessageDTO;
import com.marcos.leairning.ai.chat.dto.ChatRequestDTO;
import com.marcos.leairning.ai.chat.dto.ChatResponseDTO;
import com.marcos.leairning.ai.chat.service.ChatService;
import com.marcos.leairning.ai.chat.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    private final ChatMemory chatMemory;
    private final ConversationService conversationService;
    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;

    public ChatServiceImpl(ChatMemory chatMemory, VectorStore vectorStore, ChatClient.Builder chatClientBuilder, ConversationService conversationService) {
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;
        this.chatClientBuilder = chatClientBuilder;
        this.conversationService = conversationService;
    }

    @Override
    public ChatResponseDTO askQuestion(ChatRequestDTO request, UUID userId, UUID conversationId, String language) {
        var compositeId = userId + "_" + conversationId;
        var documentIds = getDocuments(userId, conversationId);
        var feb = new FilterExpressionBuilder();
        var filterExpression = feb.and(
                feb.eq("userId", userId.toString()),
                feb.in("documentId", (Object[]) documentIds.stream().map(UUID::toString).toArray(String[]::new))
        ).build();
        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.3)
                .topK(10)
                .filterExpression(filterExpression)
                .build();

        var queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .build();

        var retrievalAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)
                .documentRetriever(documentRetriever)
                .build();

        var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        var chatClient = chatClientBuilder.clone()
                .defaultSystem(systemSpec -> systemSpec.text(systemPromptTemplate).param("language", language))
                .defaultAdvisors(memoryAdvisor, retrievalAdvisor)
                .build();

        log.info("[LLM REQUEST] User question: {}", request.question());
        log.info("[LLM REQUEST] Document IDs: {}", documentIds);
        log.info("[LLM REQUEST] Filter: userId={}, documentIds={}", userId, documentIds);

        var chatResponse = chatClient.prompt()
                .user(request.question())
                .advisors(a -> a.param(CONVERSATION_ID, compositeId))
                .call()
                .chatResponse();

        var answer = chatResponse.getResult().getOutput().getText();

        log.info("[LLM RESPONSE] Answer: {}", answer);
        log.info("[LLM RESPONSE] Metadata: {}", chatResponse.getMetadata());
        log.info("Chat response generated for conversationId={}", compositeId);

        return new ChatResponseDTO(answer, conversationId.toString(), Instant.now());
    }

    @Override
    public List<ChatMessageDTO> getMessages(UUID userId, UUID conversationId) {
        var compositeId = userId + "_" + conversationId;
        var messages = chatMemory.get(compositeId);
        return messages.stream()
                .map(msg -> new ChatMessageDTO(
                        null,
                        msg.getMessageType().getValue(),
                        msg.getText(),
                        null))
                .toList();
    }

    private Set<UUID> getDocuments(UUID userId, UUID conversationId) {
        return conversationService.getDocumentIds(userId, conversationId);
    }
}
