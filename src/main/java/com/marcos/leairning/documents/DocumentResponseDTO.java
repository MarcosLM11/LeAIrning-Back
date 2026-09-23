package com.marcos.leairning.documents;

import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

@Builder
public record DocumentResponseDTO(
        UUID id,
        UUID userId,
        String fileName,
        String contentType,
        Long size,
        String storagePath,
        String thumbnailPath,
        DocumentStatus status,
        Instant createdTimestamp
) {}