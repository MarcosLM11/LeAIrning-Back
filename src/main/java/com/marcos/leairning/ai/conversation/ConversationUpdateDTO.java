package com.marcos.leairning.ai.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationUpdateDTO(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title
) {}
