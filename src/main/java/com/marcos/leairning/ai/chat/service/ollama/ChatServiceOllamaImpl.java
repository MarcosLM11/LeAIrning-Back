package com.marcos.leairning.ai.chat.service.ollama;

import com.marcos.leairning.ai.chat.config.TranslateResponsePostProcessor;
import com.marcos.leairning.ai.chat.dto.ChatMessageDTO;
import com.marcos.leairning.ai.chat.dto.ChatRequestDTO;
import com.marcos.leairning.ai.chat.dto.ChatResponseDTO;
import com.marcos.leairning.ai.chat.service.ChatService;
import com.marcos.leairning.ai.chat.service.ConversationService;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
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

@Flogger
@Service
public class ChatServiceOllamaImpl implements ChatService {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    private final ChatMemory chatMemory;
    private final ConversationService conversationService;
    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;

    public ChatServiceOllamaImpl(ChatMemory chatMemory, VectorStore vectorStore, ChatClient.Builder chatClientBuilder, ConversationService conversationService) {
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;
        this.chatClientBuilder = chatClientBuilder;
        this.conversationService = conversationService;
    }

    @Override
    public ChatResponseDTO askQuestion(ChatRequestDTO request, UUID userId, UUID conversationId) {
        val compositeId = userId + "_" + conversationId;
        val documentIds = getDocuments(userId, conversationId);
        var feb = new FilterExpressionBuilder();
        val filterExpression = feb.and(
                feb.eq("userId", userId.toString()),
                feb.in("documentId", documentIds.stream().map(UUID::toString).toArray())
        ).build();
        val chatClient = chatClientBuilder.clone()
                .defaultSystem(systemPromptTemplate)
                .defaultAdvisors(RetrievalAugmentationAdvisor.builder()
                        .queryTransformers(
                                RewriteQueryTransformer.builder()
                                        .chatClientBuilder(chatClientBuilder.clone())
                                        .targetSearchSystem("vector store")
                                        .build()
                        )
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .filterExpression(filterExpression)
                                .similarityThreshold(0.5)
                                .topK(3)
                                .build())
                        .documentPostProcessors(
                                TranslateResponsePostProcessor.builder()
                                        .chatClientBuilder(chatClientBuilder)
                                        .build()
                        )
                        .build())
                .build();
        val answer = chatClient.prompt()
                .user(request.question())
                .advisors(a -> a.param(CONVERSATION_ID, compositeId))
                .call()
                .content();
        log.atInfo().log("Chat response generated for conversationId=%s", compositeId);
        return new ChatResponseDTO(answer, conversationId.toString(), Instant.now());
    }

    @Override
    public List<ChatMessageDTO> getMessages(UUID userId, UUID conversationId) {
        val compositeId = userId + "_" + conversationId;
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
