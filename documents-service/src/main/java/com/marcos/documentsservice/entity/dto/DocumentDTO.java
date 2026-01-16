package com.marcos.documentsservice.entity.dto;

import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import java.time.LocalDateTime;

public record DocumentDTO(
        Long id,
        String originalFilename,
        String contentType,
        Long fileSize,
        DocumentType documentType,
        ProcessingStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}