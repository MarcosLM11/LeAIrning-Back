package com.marcos.chatservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Question cannot be blank")
        String question
) {}