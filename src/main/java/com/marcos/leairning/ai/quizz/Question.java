package com.marcos.leairning.ai.quizz;

public record Question(
        String question,
        String answer,
        QuestionType type
) {
}
