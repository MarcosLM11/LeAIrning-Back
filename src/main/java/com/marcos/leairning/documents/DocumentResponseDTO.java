package com.marcos.leairning.documents;

import java.util.UUID;

public record DocumentResponseDTO(
        UUID id,
        UUID userId,
        String fileName,
        String contentType,
        Long size,
        String storagePath
) {
}
