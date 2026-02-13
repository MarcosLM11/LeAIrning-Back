package com.marcos.leairning.ai.chat.config;

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.util.PromptAssert;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import java.util.List;

/**
 * Uses a large language model to translate document content to the same language as the user query.
 * This is useful when you want the RAG responses to be in the same language as the user's question,
 * regardless of the original document language.
 */
public class TranslateResponsePostProcessor implements DocumentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TranslateResponsePostProcessor.class);

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
        Given the following contextual information and input query, your task is to translate
        the contextual information to the same language as the input query.
        
        Contextual information:
        {context}
        
        User query:
        {query}
        
        Translated contextual information:
        """);

    private static final String DEFAULT_TARGET_LANGUAGE = "en";

    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;
    private final LanguageDetector languageDetector;

    private TranslateResponsePostProcessor(ChatClient.Builder chatClientBuilder, 
                                          @Nullable PromptTemplate promptTemplate) {
        Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");

        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
        this.languageDetector = initLanguageDetector();

        PromptAssert.templateHasRequiredPlaceholders(this.promptTemplate, "context", "query");
    }

    private LanguageDetector initLanguageDetector() {
        try {
            OptimaizeLangDetector detector = new OptimaizeLangDetector();
            detector.loadModels();
            logger.debug("Language detector initialized successfully");
            return detector;
        } catch (Exception _) {
            logger.error("Failed to initialize language detector");
        }
        return null;
    }

    @Override
    public @NonNull List<Document> process(@NonNull Query query, @NonNull List<Document> documents) {
        Assert.notNull(query, "query cannot be null");
        Assert.notNull(documents, "documents cannot be null");
        Assert.noNullElements(documents, "documents cannot contain null elements");

        if (CollectionUtils.isEmpty(documents)) {
            return documents;
        }

        String queryText = query.text();
        String targetLanguage = detectLanguage(queryText);
        
        logger.debug("Translating documents to language '{}' for query: {}", targetLanguage, queryText);

        return documents.stream()
                .map(document -> {
                    String documentText = document.getText();
                    if (documentText == null || documentText.isBlank()) {
                        return document;
                    }

                    // Detect document language
                    String documentLanguage = detectLanguage(documentText);
                    
                    // If a document is already in the target language, skip translation
                    if (documentLanguage.equals(targetLanguage)) {
                        logger.debug("Document already in target language '{}', skipping translation", targetLanguage);
                        return document;
                    }

                    logger.debug("Translating document from '{}' to '{}'", documentLanguage, targetLanguage);

                    // Translate document content
                    String translatedText = translateContent(documentText, queryText);

                    return document.mutate()
                            .text(translatedText)
                            .metadata("originalLanguage", documentLanguage)
                            .metadata("translatedTo", targetLanguage)
                            .build();
                })
                .toList();
    }

    /**
     * Detects the language of the given text using Apache Tika.
     * 
     * @param text the text to analyze
     * @return the ISO 639-1 language code (e.g., "en", "es", "fr")
     */
    private String detectLanguage(String text) {
        try {
            LanguageResult result = languageDetector.detect(text);
            String language = result.getLanguage();
            
            if (language == null || language.isBlank() || "unknown".equalsIgnoreCase(language)) {
                logger.debug("Could not detect language, defaulting to '{}'", DEFAULT_TARGET_LANGUAGE);
                return DEFAULT_TARGET_LANGUAGE;
            }
            
            logger.debug("Detected language: '{}' with confidence: {}", 
                        language, result.getRawScore());
            return language;
        } catch (Exception e) {
            logger.warn("Error detecting language, defaulting to '{}': {}", DEFAULT_TARGET_LANGUAGE, e.getMessage());
            return DEFAULT_TARGET_LANGUAGE;
        }
    }

    /**
     * Translates the content to the same language as the query using the LLM.
     * 
     * @param content the content to translate
     * @param query the user query (used to determine the target language)
     * @return the translated content
     */
    private String translateContent(String content, String query) {
        try {
            return chatClient.prompt()
                    .user(user -> user.text(this.promptTemplate.getTemplate())
                            .param("context", content)
                            .param("query", query))
                    .options(ChatOptions.builder()
                            .temperature(0.3)
                            .build())
                    .call()
                    .content();
        } catch (Exception e) {
            logger.error("Error translating content, returning original: {}", e.getMessage());
            return content;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ChatClient.Builder chatClientBuilder;
        private PromptTemplate promptTemplate = DEFAULT_PROMPT_TEMPLATE;

        private Builder() {}

        public Builder chatClientBuilder(ChatClient.Builder chatClientBuilder) {
            this.chatClientBuilder = chatClientBuilder;
            return this;
        }

        public Builder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public TranslateResponsePostProcessor build() {
            Assert.notNull(chatClientBuilder, "chatClientBuilder must not be null");
            return new TranslateResponsePostProcessor(this.chatClientBuilder, this.promptTemplate);
        }
    }
}
