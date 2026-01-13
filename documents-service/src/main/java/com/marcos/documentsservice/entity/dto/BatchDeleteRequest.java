package com.marcos.documentsservice.entity.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchDeleteRequest(
        @NotEmpty(message = "Document IDs list cannot be empty")
        List<Long> documentIds
) {
}