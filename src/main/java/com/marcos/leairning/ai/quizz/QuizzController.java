package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.security.annotations.BusinessAuthorityOnly;
import com.marcos.leairning.util.web.CurrentUserId;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Flogger
@BusinessAuthorityOnly
@RestController
@RequestMapping("/quizz")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class QuizzController {

    QuizzService quizzService;

    @PostMapping(path = "/generate/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneratedQuizz> generate(
            @CurrentUserId UUID userId,
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "5") int numberOfQuestions,
            @RequestParam(defaultValue = "MEDIUM") QuestionType difficulty) {
        log.atInfo().log("Quiz request from userId=%s, documentId=%s, questions=%d, difficulty=%s",
                userId, documentId, numberOfQuestions, difficulty);
        val quizz = quizzService.generateQuizz(userId, documentId, numberOfQuestions, difficulty);
        return ResponseEntity.ok(quizz);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<QuizzEntity>> getUserQuizzs(@CurrentUserId UUID userId, Pageable pageable) {
        return ResponseEntity.ok(quizzService.getUserQuizzs(userId, pageable));
    }

    @GetMapping(path = "/{quizzId}", produces = MediaType.APPLICATION_JSON_VALUE)
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
