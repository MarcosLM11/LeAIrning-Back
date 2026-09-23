package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizzService {

    private static final int MAX_CHUNKS = 20;
    private static final int MIN_CHUNKS = 5;
    private static final List<String> DIVERSITY_QUERIES = List.of(
            "definitions and key concepts",
            "important facts and details",
            "core mechanisms and explanations",
            "examples and applications",
            "technical details"
    );

    private static final String SYSTEM_PROMPT = """
        You are an educational quiz generator.
        
        Your task is to generate EXACTLY {numberOfQuestions} questions based ONLY on factual information explicitly present in the provided context.

        The questions MUST match this difficulty level: {difficulty}.
        - EASY: straightforward recall of a single explicit fact or definition.
        - MEDIUM: requires connecting two related facts or explaining a mechanism.
        - HARD: requires reasoning about implications, comparisons, or multi-step mechanisms described in the text.

        STRICT RULES:

        - Each question MUST be answerable using a specific factual statement from the context.
        - DO NOT generate meta questions about the reader, chapters, intentions, benefits, or opinions.
        - DO NOT ask questions about structure (e.g., "What is the purpose of this chapter?")
        - DO NOT ask questions that cannot be answered with a concrete fact.
        - Focus ONLY on:
          - definitions
          - concepts
          - facts
          - mechanisms
          - examples described in the text
        
        - Each question MUST test knowledge of the content itself.
        
        BAD QUESTION EXAMPLES (DO NOT GENERATE):
        - Who benefits from reading this chapter?
        - What is the purpose of this text?
        - Why is this chapter important?
        
        GOOD QUESTION EXAMPLES:
        - What is X?
        - How does X work?
        - What happens when X occurs?
        - What is the definition of X?
        
        You MUST respond with ONLY valid JSON format and in the following language: {language}.
        """;

    private final ChatClient.Builder chatClientBuilder;
    private final QuizzRepository quizzRepository;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;
    private final SecureRandom random = new SecureRandom();

    public GeneratedQuizz generateQuizz(UUID userId, UUID documentId, int numberOfQuestions, QuestionType difficulty, String language) {
        var context = retrieveDiverseContext(userId, documentId, numberOfQuestions);
        var chatClient = chatClientBuilder.clone().build();
        log.info("Generating quiz: {} questions, difficulty={}, documentId={}", numberOfQuestions, difficulty, documentId);
        var response = chatClient.prompt()
                .system(s -> s.text(SYSTEM_PROMPT)
                        .param("numberOfQuestions", numberOfQuestions)
                        .param("difficulty", difficulty.name())
                        .param("language", language))
                .user(context)
                .call()
                .entity(Quizz.class);
        var entity = new QuizzEntity();
        entity.setDocumentId(documentId);
        entity.setUserId(userId);
        entity.setQuiz(objectMapper.writeValueAsString(response));
        quizzRepository.save(entity);
        return new GeneratedQuizz(entity.getId(), response.questions());
    }

    public Page<QuizzEntity> getUserQuizzs(UUID userId, Pageable pageable) {
        return quizzRepository.findAllByUserId(userId, pageable);
    }

    public Quizz getQuizz(UUID userId, UUID quizzId) {
        var entity = findByIdAndUserIdOrThrow(quizzId, userId);
        return objectMapper.readValue(entity.getQuiz(), Quizz.class);
    }

    public void deleteQuizz(UUID userId, UUID quizzId) {
        findByIdAndUserIdOrThrow(quizzId, userId);
        quizzRepository.deleteByIdAndUserId(quizzId, userId);
    }

    public void updateQuizzScore(UUID userId, UUID quizzId, int score) {
        var entity = findByIdAndUserIdOrThrow(quizzId, userId);
        entity.setLastScore(score);
        quizzRepository.save(entity);
    }

    private QuizzEntity findByIdAndUserIdOrThrow(UUID quizzId, UUID userId) {
        return quizzRepository.findByIdAndUserId(quizzId, userId)
                .orElseThrow(() -> new NotFoundException("Quizz not found: " + quizzId));
    }

    private String retrieveDiverseContext(UUID userId, UUID documentId, int numberOfQuestions) {
        var feb = new FilterExpressionBuilder();
        var filter = feb.and(
                feb.eq("userId", userId.toString()),
                feb.eq("documentId", documentId.toString())
        ).build();

        var retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.15)
                .topK(Math.clamp(numberOfQuestions * 4L, MIN_CHUNKS, MAX_CHUNKS))
                .filterExpression(filter)
                .build();

        var chunks = DIVERSITY_QUERIES.stream()
                .flatMap(query -> retriever.retrieve(new Query(query)).stream())
                .map(doc -> doc.getText().replaceAll("\\s+", " ").trim())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(chunks, random);
        var selected = chunks.subList(0, Math.min(chunks.size(), numberOfQuestions * 3));
        log.info("Selected {} chunks for quiz", selected.size());
        return String.join("\n\n", selected);
    }
}