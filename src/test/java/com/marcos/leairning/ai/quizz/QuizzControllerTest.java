package com.marcos.leairning.ai.quizz;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizzControllerTest {

    QuizzService quizzService;
    QuizzController controller;

    @BeforeEach
    void setUp() {
        quizzService = mock(QuizzService.class);
        controller = new QuizzController(quizzService);
    }

    @Test
    void generate_returnsOkWithQuizz() {
        val userId = UUID.randomUUID();
        val docId = UUID.randomUUID();
        val questions = List.of(new Question("What?", "Answer", QuestionType.MEDIUM));
        val generated = new GeneratedQuizz(UUID.randomUUID(), questions);
        when(quizzService.generateQuizz(userId, docId, 5, QuestionType.MEDIUM, "es")).thenReturn(generated);
        val response = controller.generate(userId, docId, 5, QuestionType.MEDIUM, "es");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(generated, response.getBody());
    }

    @Test
    void getUserQuizzs_returnsPage() {
        val userId = UUID.randomUUID();
        val pageable = PageRequest.of(0, 20);
        val entity = new QuizzEntity();
        entity.setUserId(userId);
        val page = new PageImpl<>(List.of(entity));
        when(quizzService.getUserQuizzs(userId, pageable)).thenReturn(page);
        val response = controller.getUserQuizzs(userId, pageable);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    void getQuizz_returnsQuizz() {
        val userId = UUID.randomUUID();
        val quizzId = UUID.randomUUID();
        val quizz = new Quizz(List.of(new Question("Q?", "A", QuestionType.EASY)));
        when(quizzService.getQuizz(userId, quizzId)).thenReturn(quizz);
        val response = controller.getQuizz(userId, quizzId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(quizz, response.getBody());
    }

    @Test
    void deleteQuizz_returnsNoContent() {
        val userId = UUID.randomUUID();
        val quizzId = UUID.randomUUID();
        val response = controller.deleteQuizz(userId, quizzId);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(quizzService).deleteQuizz(userId, quizzId);
    }

    @Test
    void updateQuizzScore_returnsNoContent() {
        val userId = UUID.randomUUID();
        val quizzId = UUID.randomUUID();
        val response = controller.updateQuizzScore(userId, quizzId, 85);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(quizzService).updateQuizzScore(userId, quizzId, 85);
    }
}
