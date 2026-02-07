package com.marcos.leairning.ai.chat;

import com.marcos.leairning.ai.conversation.ConversationService;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
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
import java.util.concurrent.atomic.AtomicInteger;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Flogger
@Service
public class ChatServiceImpl implements ChatService {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    private final ChatClient client;
    private final ChatMemoryRepository chatMemoryRepository;
    private final VectorStore vectorStore;
    private final ConversationService conversationService;

    public ChatServiceImpl(
            ChatClient client,
            ChatMemoryRepository chatMemoryRepository,
            VectorStore vectorStore,
            ConversationService conversationService
    ) {
        this.client = client;
        this.chatMemoryRepository = chatMemoryRepository;
        this.vectorStore = vectorStore;
        this.conversationService = conversationService;
    }

    @Override
    public ChatResponseDTO askQuestion(String question, UUID userId, String conversationId) {
        log.atInfo().log("Processing question for userId=%s, conversationId=%s", userId, conversationId);
        
        val userConversationId = userId + "_" + conversationId;
        val documentIds = resolveDocumentIds(userId, conversationId);
        val filterExpression = buildUserFilter(userId, documentIds);
        val ragAdvisor = buildRagAdvisor(filterExpression);
        val answer = generateAnswer(question, userConversationId, ragAdvisor);
        
        return new ChatResponseDTO(answer, conversationId, Instant.now());
    }

    private Set<UUID> resolveDocumentIds(UUID userId, String conversationId) {
        try {
            return conversationService.getDocumentIds(userId, UUID.fromString(conversationId));
        
        } catch (IllegalArgumentException _) {
            log.atWarning().log("Invalid conversationId format, falling back to all user documents");
            return Set.of();
        }
    }

    private Filter.Expression buildUserFilter(UUID userId, Set<UUID> documentIds) {
        return buildFilterExpression(userId, documentIds);
    }

    private RetrievalAugmentationAdvisor buildRagAdvisor(Filter.Expression filterExpression) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .filterExpression(filterExpression)
                        .similarityThreshold(0.5)
                        .topK(5)
                        .build())
                .build();
    }

    private String generateAnswer(String question, String conversationId, RetrievalAugmentationAdvisor ragAdvisor) {

        return client.prompt()
                .system(systemPromptTemplate)
                .user(question)
                .advisors(ragAdvisor)
                .advisors(spec -> spec.param(CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    @Override
    public List<ChatMessageDTO> getMessages(UUID userId, UUID conversationId) {
        log.atInfo().log("Getting messages for userId=%s, conversationId=%s", userId, conversationId);

        val userConversationId = userId + "_" + conversationId;

        List<Message> messages = chatMemoryRepository.findByConversationId(userConversationId);

        val counter = new AtomicInteger(0);
        return messages.stream()
                .filter(msg -> msg.getMessageType() == MessageType.USER ||
                               msg.getMessageType() == MessageType.ASSISTANT)
                .map(msg -> new ChatMessageDTO(
                        userConversationId + "_" + counter.getAndIncrement(),
                        mapMessageType(msg.getMessageType()),
                        msg.getText(),
                        Instant.now()
                ))
                .toList();
    }

    private String mapMessageType(MessageType type) {
        return switch (type) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

    private Filter.Expression buildFilterExpression(UUID userId, Set<UUID> documentIds) {
        val builder = new FilterExpressionBuilder();

        if (documentIds == null || documentIds.isEmpty()) {
            return builder.eq("userId", userId.toString()).build();
        }

        val documentIdStrings = documentIds.stream()
                .map(UUID::toString)
                .toList();

        return builder.and(
                builder.eq("userId", userId.toString()),
                builder.in("documentId", documentIdStrings.toArray(new String[0]))
        ).build();
    }
}
