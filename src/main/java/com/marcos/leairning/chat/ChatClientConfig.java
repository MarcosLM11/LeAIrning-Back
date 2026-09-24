package com.marcos.leairning.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.RecursiveSummarizationCompactionStrategy;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatClientConfig {

    @Bean
    @Primary
    ChatClient.Builder openAiChatClientBuilder(
            OpenAiChatModel chatModel,
            ObjectProvider<ChatClientBuilderCustomizer> customizers) {
        return applyCustomizers(ChatClient.builder(chatModel), customizers);
    }

    @Bean
    @Qualifier("queryCompressionChatClientBuilder")
    ChatClient.Builder compressionChatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(OpenAiChatOptions
                        .builder().model("gpt-5-nano"));
    }

    private ChatClient.Builder applyCustomizers(
            ChatClient.Builder builder,
            ObjectProvider<ChatClientBuilderCustomizer> customizers) {
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    SessionService sessionService() {
        return DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build())
                .build();
    }

    @Bean
    ChatClientBuilderCustomizer chatMemoryCustomizer(SessionService sessionService, ChatModel chatModel) {
        return builder -> {
            var chatClient = ChatClient.builder(chatModel).build();
            builder.defaultAdvisors(
                    SessionMemoryAdvisor.builder(sessionService)
                            .defaultUserId("marcos")
                            .compactionTrigger(new TurnCountTrigger(20))
                            .compactionStrategy(
                                    RecursiveSummarizationCompactionStrategy.builder(chatClient)
                                            .maxEventsToKeep(10)
                                            .build())
                            .build());
        };
    }

    @Bean
    QueryTransformer queryTransformer(@Qualifier("queryCompressionChatClientBuilder") ChatClient.Builder chatClientBuilder) {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
    }

    @Bean
    ChatClientBuilderCustomizer addRagAdvisor(QueryTransformer queryTransformer, VectorStore vectorStore) {
        return builder -> {
            var ragAdvisor = RetrievalAugmentationAdvisor.builder()
                    .queryTransformers(queryTransformer)
                    .documentRetriever(VectorStoreDocumentRetriever.builder()
                            .vectorStore(vectorStore)
                            .build())
                    .build();
            builder.defaultAdvisors(ragAdvisor);
        };
    }
}