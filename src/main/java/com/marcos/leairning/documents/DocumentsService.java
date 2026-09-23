package com.marcos.leairning.documents;

import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import com.marcos.leairning.etl.ChunkingService;
import com.marcos.leairning.exception.DocumentProcessingException;
import com.marcos.leairning.exception.NotFoundException;
import com.marcos.leairning.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import static com.marcos.leairning.cache.CaffeineCacheProperties.DEFAULT_POLICY;

@Slf4j
@Service
@RateLimiting(name = DEFAULT_POLICY)
@RequiredArgsConstructor
public class DocumentsService {

    private final DocumentsRepository repository;
    private final MinioService minioService;
    private final VectorStore vectorStore;
    private final ApplicationEventPublisher eventPublisher;

    public Page<DocumentResponseDTO> getDocuments(UUID userId, Pageable pageable) {
        log.info("Fetching documents for user {}", userId);
        return repository.findByUserId(userId, pageable).map(this::mapToDto);
    }

    @Transactional
    public List<DocumentResponseDTO> upload(UUID userId, List<MultipartFile> files) {
        log.info("User {} uploading {} documents", userId, files.size());
        return files.stream().map(file -> uploadDocument(userId, file)).toList();
    }

    @Cacheable(value = "documents", key = "#userId + '-' + #documentId")
    public DocumentResponseDTO getDocument(UUID userId, UUID documentId) {
        log.info("Fetching document {} for user {}", documentId, userId);
        return mapToDto(findDocumentWithOwnershipValidation(documentId, userId));
    }

    @Transactional
    @CacheEvict(value = "documents", key = "#userId + '-' + #documentId")
    public void deleteDocument(UUID userId, UUID documentId) {
        log.info("User {} deleting document {}", userId, documentId);
        var document = findDocumentWithOwnershipValidation(documentId, userId);
        minioService.delete(document.getStoragePath());
        repository.deleteById(documentId);
        deleteFromVectorStore(documentId);
    }

    public Resource downloadDocument(UUID userId, UUID documentId) {
        log.info("User {} downloading document {}", userId, documentId);
        var document = findDocumentWithOwnershipValidation(documentId, userId);
        return minioService.download(document.getStoragePath());
    }

    @Transactional
    public void deleteDocuments(UUID userId, List<UUID> documentIds) {
        log.info("User {} batch deleting {} documents", userId, documentIds.size());
        var documentsToDelete = repository.findByIdInAndUserId(documentIds, userId);
        if (documentsToDelete.isEmpty()) return;

        documentsToDelete.forEach(doc -> minioService.delete(doc.getStoragePath()));
        documentsToDelete.forEach(doc -> deleteFromVectorStore(doc.getId()));

        var idsToDelete = documentsToDelete.stream().map(Document::getId).toList();
        repository.deleteByIdInAndUserId(idsToDelete, userId);
    }

    private DocumentResponseDTO uploadDocument(UUID userId, MultipartFile file) {
        var filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        var contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        var storagePath = "documents/%s/%s_%s".formatted(userId, UUID.randomUUID(), filename);

        var document = Document.builder()
                .fileName(filename)
                .contentType(contentType)
                .size(file.getSize())
                .status(DocumentStatus.UPLOADED)
                .storagePath(storagePath)
                .userId(userId)
                .build();
        try {
            minioService.upload(storagePath, file.getInputStream());
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read file bytes", e);
        }

        var saved = repository.save(document);
        eventPublisher.publishEvent(new DocumentUploadedEvent(saved.getId()));
        log.info("Document {} uploaded by user {}", saved.getId(), userId);
        return mapToDto(saved);
    }

    private void deleteFromVectorStore(UUID documentId) {
        vectorStore.delete(new FilterExpressionBuilder()
                .eq(ChunkingService.METADATA_DOCUMENT_ID, documentId.toString())
                .build());
    }

    private Document findDocumentWithOwnershipValidation(UUID documentId, UUID userId) {
        return repository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
    }

    private DocumentResponseDTO mapToDto(Document document) {
        return DocumentResponseDTO.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .size(document.getSize())
                .status(document.getStatus())
                .storagePath(document.getStoragePath())
                .createdTimestamp(document.getCreatedTimestamp())
                .build();
    }
}