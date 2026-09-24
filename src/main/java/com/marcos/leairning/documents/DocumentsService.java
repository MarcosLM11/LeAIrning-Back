package com.marcos.leairning.documents;

import com.marcos.leairning.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DocumentsService {

    private final DocumentsRepository repository;

    @Transactional
    public DocumentResponseDTO upload(UUID userId, MultipartFile file) {
        log.info("User {} uploading document {}", userId, file.getOriginalFilename());
        var filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        var contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        var document = Document.builder()
                .userId(userId)
                .fileName(filename)
                .contentType(contentType)
                .size(file.getSize())
                .content(readBytes(file))
                .build();
        return mapToDto(repository.save(document));
    }

    public List<DocumentResponseDTO> getDocuments(UUID userId) {
        return repository.findByUserId(userId).stream().map(DocumentsService::mapToDto).toList();
    }

    public DocumentResponseDTO getDocument(UUID userId, UUID documentId) {
        return mapToDto(findDocumentOrThrow(userId, documentId));
    }

    public Resource downloadDocument(UUID userId, UUID documentId) {
        return new ByteArrayResource(findDocumentOrThrow(userId, documentId).getContent());
    }

    @Transactional
    public void deleteDocument(UUID userId, UUID documentId) {
        var document = findDocumentOrThrow(userId, documentId);
        repository.delete(document);
        log.info("User {} deleted document {}", userId, documentId);
    }

    private Document findDocumentOrThrow(UUID userId, UUID documentId) {
        return repository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file bytes", e);
        }
    }

    private static DocumentResponseDTO mapToDto(Document document) {
        return DocumentResponseDTO.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .size(document.getSize())
                .createdTimestamp(document.getCreatedTimestamp())
                .build();
    }
}