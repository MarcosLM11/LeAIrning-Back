package com.marcos.leairning.ai.quizz;

import java.util.List;
import java.util.UUID;

public record GeneratedQuizz(
        UUID id,
        List<Question> questions
) {
}
