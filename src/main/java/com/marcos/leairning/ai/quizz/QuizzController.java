package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.util.web.CurrentUserId;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Flogger
@RestController
@RequestMapping("/quizz")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class QuizzController {

    QuizzService quizzService;

    @PostMapping(path = "/generate/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Quizz> generate(
            @CurrentUserId UUID userId,
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "5") int numberOfQuestions,
            @RequestParam(defaultValue = "MEDIUM") QuestionType difficulty) {
        log.atInfo().log("Quiz request from userId=%s, documentId=%s, questions=%d, difficulty=%s",
                userId, documentId, numberOfQuestions, difficulty);
        val quizz = quizzService.generateQuizz(userId, documentId, numberOfQuestions, difficulty);
        return ResponseEntity.ok(quizz);
    }
}