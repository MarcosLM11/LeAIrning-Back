package com.marcos.leairning.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank(message = "Question cannot be blank")
        String question
) {}
