package com.marcos.leairning.ai.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record ConversationRequestDTO(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotEmpty(message = "At least one document must be selected")
        Set<UUID> documentIds
) {}
