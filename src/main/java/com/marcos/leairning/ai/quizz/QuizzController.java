package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.security.annotations.BusinessAuthorityOnly;
import com.marcos.leairning.util.web.CurrentUserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Slf4j
@BusinessAuthorityOnly
@RestController
@RequestMapping("/quizz")
@RequiredArgsConstructor
public class QuizzController {

    private final QuizzService quizzService;

    @PostMapping(path = "/generate/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneratedQuizz> generate(
            @CurrentUserId UUID userId,
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "5") int numberOfQuestions,
            @RequestParam(defaultValue = "MEDIUM") QuestionType difficulty,
            @RequestParam(defaultValue = "es") String language) {
        log.info("Quiz request from userId={}, documentId={}, questions={}, difficulty={}, language={}", userId, documentId, numberOfQuestions, difficulty, language);
        var quiz = quizzService.generateQuizz(userId, documentId, numberOfQuestions, difficulty, language);
        return ResponseEntity.ok(quiz);
    }

    @GetMapping
    public ResponseEntity<Page<QuizzEntity>> getUserQuizzs(@CurrentUserId UUID userId, Pageable pageable) {
        return ResponseEntity.ok(quizzService.getUserQuizzs(userId, pageable));
    }

    @GetMapping(path = "/{quizzId}")
    public ResponseEntity<Quizz> getQuizz(@CurrentUserId UUID userId, @PathVariable UUID quizzId) {
        return ResponseEntity.ok(quizzService.getQuizz(userId, quizzId));
    }

    @DeleteMapping("/{quizzId}")
    public ResponseEntity<Void> deleteQuizz(@CurrentUserId UUID userId, @PathVariable UUID quizzId) {
        quizzService.deleteQuizz(userId, quizzId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{quizzId}/score")
    public ResponseEntity<Void> updateQuizzScore(@CurrentUserId UUID userId, @PathVariable UUID quizzId, @RequestParam int score) {
        quizzService.updateQuizzScore(userId, quizzId, score);
        return ResponseEntity.noContent().build();
    }
}