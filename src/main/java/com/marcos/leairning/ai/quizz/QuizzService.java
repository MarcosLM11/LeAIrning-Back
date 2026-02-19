package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.documents.DocumentsService;
import com.marcos.leairning.exception.QuizzNotFoundException;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.stream.Collectors;

@Flogger
@Service
public class QuizzService {

    private static final String SYSTEM_PROMPT = """
          You are a quiz generator for educational purposes.
          Generate exactly {numberOfQuestions} questions at {difficulty} difficulty level \
          based on the provided document content.
          Each question must have a clear, concise answer derived from the document.
          Set the type field of each question to {difficulty}.
          You MUST respond with ONLY valid JSON, no additional text before or after.
          The JSON must be an object with a "questions" array. Each element must have \
          "question", "answer", and "type" fields.
          Do not include numbering, markdown, or any text outside the JSON.""";

    private final DocumentsService documentsService;
    private final ChatClient.Builder chatClientBuilder;
    private final QuizzRepository quizzRepository;
    private final ObjectMapper objectMapper;

    public QuizzService(QuizzRepository quizzRepository, DocumentsService documentsService, ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.quizzRepository = quizzRepository;
        this.documentsService = documentsService;
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
    }

    public GeneratedQuizz generateQuizz(UUID userId, UUID documentId, int numberOfQuestions, QuestionType difficulty) {
        val content = extractDocumentContent(userId, documentId);
        val chatClient = chatClientBuilder.clone().build();
        log.atInfo().log("Generating quiz: %d questions, difficulty=%s, documentId=%s",
                numberOfQuestions, difficulty, documentId);
        val response = chatClient.prompt()
                .system(s -> s.text(SYSTEM_PROMPT)
                        .param("numberOfQuestions", numberOfQuestions)
                        .param("difficulty", difficulty.name()))
                .user(content)
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

    private String extractDocumentContent(UUID userId, UUID documentId) {
        var bytes = documentsService.downloadDocument(userId, documentId);
        var documents = new TikaDocumentReader(new ByteArrayResource(bytes)).get();
        return documents.stream()
                .map(org.springframework.ai.document.Document::getText)
                .collect(Collectors.joining("\n"));
    }
}
