package com.marcos.documentsservice.entity.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record UploadDocumentRequest(
        List<MultipartFile> files
) {
}