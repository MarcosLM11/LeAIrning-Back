package com.marcos.documentsservice.entity.dto;

import java.util.List;

public record UploadDocumentResponse(
        List<DocumentDTO> documents,
        String message
) {
}