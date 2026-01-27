package com.marcos.leairning.documents;

import java.util.UUID;

public record DocumentResponseDTO(
        UUID id,
        UUID user,
        String fileName,
        String contentType,
        Long size,
        String storagePath
) {
}
