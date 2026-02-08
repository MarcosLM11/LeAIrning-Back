package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.documents.DocumentsService;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
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
                        Set the type field of each question to {difficulty}.""";

    private final DocumentsService documentsService;
    private final ChatClient.Builder chatClientBuilder;

    public QuizzService(DocumentsService documentsService, ChatClient.Builder chatClientBuilder) {
        this.documentsService = documentsService;
        this.chatClientBuilder = chatClientBuilder;
    }

    public Quizz generateQuizz(UUID userId, UUID documentId, int numberOfQuestions, QuestionType difficulty) {
        val content = extractDocumentContent(userId, documentId);
        val chatClient = chatClientBuilder.clone().build();
        log.atInfo().log("Generating quiz: %d questions, difficulty=%s, documentId=%s",
                numberOfQuestions, difficulty, documentId);
        return chatClient.prompt()
                .system(s -> s.text(SYSTEM_PROMPT)
                        .param("numberOfQuestions", numberOfQuestions)
                        .param("difficulty", difficulty.name()))
                .user(content)
                .call()
                .entity(Quizz.class);
    }

    private String extractDocumentContent(UUID userId, UUID documentId) {
        var bytes = documentsService.downloadDocument(userId, documentId);
        var documents = new TikaDocumentReader(new ByteArrayResource(bytes)).get();
        return documents.stream()
                .map(org.springframework.ai.document.Document::getText)
                .collect(Collectors.joining("\n"));
    }
}