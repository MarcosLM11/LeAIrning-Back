package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.documents.DocumentsService;
import com.marcos.leairning.exception.QuizzNotFoundException;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Flogger
@Service
public class QuizzService {

    private static final int MAX_CHUNKS = 20;
    private static final int MIN_CHUNKS = 5;
    private static final int CHARS_PER_CHUNK_ESTIMATE = 800;

    private static final String SYSTEM_PROMPT = """
        You are an educational quiz generator.
        
        Your task is to generate EXACTLY {numberOfQuestions} questions based ONLY on factual information explicitly present in the provided context.
        
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
        
        You MUST respond with ONLY valid JSON format
        """;

    private final DocumentsService documentsService;
    private final ChatClient.Builder chatClientBuilder;
    private final QuizzRepository quizzRepository;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    private final SecureRandom random = new SecureRandom();

    public QuizzService(QuizzRepository quizzRepository, DocumentsService documentsService, ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, VectorStore vectorStore) {
        this.quizzRepository = quizzRepository;
        this.documentsService = documentsService;
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
    }

    public GeneratedQuizz generateQuizz(UUID userId, UUID documentId, int numberOfQuestions, QuestionType difficulty) {
        val context = retrieveDiverseContext(userId, documentId, numberOfQuestions);
        val chatClient = chatClientBuilder.clone().build();
        log.atInfo().log("Generating quiz: %d questions, difficulty=%s, documentId=%s",
                numberOfQuestions, difficulty, documentId);
        val response = chatClient.prompt()
                .system(s -> s.text(SYSTEM_PROMPT)
                        .param("numberOfQuestions", numberOfQuestions)
                        .param("difficulty", difficulty.name()))
                .user(context)
                .call()
                .entity(Quizz.class);
        val entity = new QuizzEntity();
        entity.setDocumentId(documentId);
        entity.setUserId(userId);
        entity.setQuizz(objectMapper.writeValueAsString(response));
        quizzRepository.save(entity);
        return new GeneratedQuizz(entity.getId(), response.questions());
    }

    public Page<QuizzEntity> getUserQuizzs(UUID userId, Pageable pageable) {
        return quizzRepository.findAllByUserId(userId, pageable);
    }

    public Quizz getQuizz(UUID userId, UUID quizzId) {
        val entity = findByIdAndUserIdOrThrow(quizzId, userId);
        return objectMapper.readValue(entity.getQuizz(), Quizz.class);
    }

    public void deleteQuizz(UUID userId, UUID quizzId) {
        findByIdAndUserIdOrThrow(quizzId, userId);
        quizzRepository.deleteByIdAndUserId(quizzId, userId);
    }

    public void updateQuizzScore(UUID userId, UUID quizzId, int score) {
        val entity = findByIdAndUserIdOrThrow(quizzId, userId);
        entity.setLastScore(score);
        quizzRepository.save(entity);
    }

    private QuizzEntity findByIdAndUserIdOrThrow(UUID quizzId, UUID userId) {
        return quizzRepository.findByIdAndUserId(quizzId, userId)
                .orElseThrow(() -> new QuizzNotFoundException("Quizz not found: " + quizzId));
    }

    private String retrieveDiverseContext(
            UUID userId,
            UUID documentId,
            int numberOfQuestions) {

        var feb = new FilterExpressionBuilder();

        var filter = feb.and(
                feb.eq("userId", userId.toString()),
                feb.eq("documentId", documentId.toString())
        ).build();

        int poolSize = Math.min(
                MAX_CHUNKS,
                Math.max(MIN_CHUNKS, numberOfQuestions * 4)
        );

        var retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.15)
                .topK(poolSize)
                .filterExpression(filter)
                .build();

        List<String> queries = List.of(
                "definitions and key concepts",
                "important facts and details",
                "core mechanisms and explanations",
                "examples and applications",
                "technical details"
        );

        Set<String> uniqueChunks = new LinkedHashSet<>();

        for (String query : queries) {

            var docs = retriever.retrieve(new Query(query));

            docs.forEach(doc ->
                    uniqueChunks.add(cleanChunk(doc.getText()))
            );
        }

        List<String> shuffled = new ArrayList<>(uniqueChunks);

        Collections.shuffle(shuffled, random);

        int maxChunksToUse = Math.min(
                shuffled.size(),
                numberOfQuestions * 3
        );

        List<String> selected = shuffled.subList(0, maxChunksToUse);

        log.atInfo().log("Selected %d chunks for quiz", selected.size());

        return String.join("\n\n", selected);
    }

    /**
     * Remove useless or noisy text
     */
    private String cleanChunk(String chunk) {

        if (chunk == null) return "";

        return chunk
                .replaceAll("\\s+", " ")
                .trim();
    }
}
