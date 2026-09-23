package com.marcos.leairning.documents;

import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import com.marcos.leairning.exception.DocumentNotFoundException;
import com.marcos.leairning.exception.DocumentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private static final long MAX_DECOMPRESSION_RATIO = 100;
    private static final Tika TIKA = new Tika();

    private final DocumentsRepository repository;
    private final MinioDocumentStorageService storageService;
    private final MinioProcessingPipelineService pipelineService;
    private final VectorStore vectorStore;

    public Page<DocumentResponseDTO> getDocuments(UUID userId, Pageable pageable) {
        log.info("Fetching documents for user {}", userId);
        return repository.findByUserId(userId, pageable).map(this::mapToDto);
    }

    @Transactional
    public List<DocumentResponseDTO> upload(UUID userId, List<MultipartFile> files) {
        log.info("User {} uploading {} documents", userId, files.size());
        var result = files.stream()
                .map(file -> uploadDocument(userId, file))
                .toList();
        log.info("Successfully uploaded {} documents for user {}", result.size(), userId);
        return result;
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
        storageService.delete(document.getStoragePath());
        storageService.delete(document.getThumbnailPath());
        repository.deleteById(documentId);
        vectorStore.delete(new FilterExpressionBuilder()
                .eq(ChunkingService.METADATA_DOCUMENT_ID, documentId.toString())
                .build());
    }

    public Resource downloadDocument(UUID userId, UUID documentId) {
        log.info("User {} downloading document {}", userId, documentId);
        var document = findDocumentWithOwnershipValidation(documentId, userId);
        return storageService.download(document.getStoragePath());
    }

    public Resource downloadThumbnail(UUID userId, UUID documentId) {
        var document = findDocumentWithOwnershipValidation(documentId, userId);
        return storageService.download(document.getThumbnailPath());
    }

    @Transactional
    public void deleteDocuments(UUID userId, List<UUID> documentIds) {
        log.info("User {} batch deleting {} documents", userId, documentIds.size());
        var documentsToDelete = repository.findByIdInAndUserId(documentIds, userId);
        if (documentsToDelete.isEmpty()) return;

        documentsToDelete.forEach(doc -> storageService.delete(doc.getStoragePath()));
        documentsToDelete.forEach(doc -> storageService.delete(doc.getThumbnailPath()));
        documentsToDelete.forEach(doc -> vectorStore.delete(new FilterExpressionBuilder()
                .eq(ChunkingService.METADATA_DOCUMENT_ID, documentId.toString())
                .build()));
        
        var idsToDelete = documentsToDelete.stream()
                .map(Document::getId)
                .toList();
        
        repository.deleteByIdInAndUserId(idsToDelete, userId);
    }

    private DocumentResponseDTO uploadDocument(UUID userId, MultipartFile file) {
        var filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        var contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        var key = "%s/%s_%s".formatted(userId, UUID.randomUUID(), file.getOriginalFilename());

        var document = Document.builder()
                .fileName(filename)
                .contentType(contentType)
                .size(file.getSize())
                .status(DocumentStatus.UPLOADED)
                .storagePath("documents/"+key)
                .thumbnailPath("thumbnail/"+key)
                .userId(userId)
                .build();
        try {
            storageService.store(file.getBytes(), document);
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read file bytes", e);
        }
        
        var saved = repository.save(document);
        eventPublisher.publishEvent(new DocumentUploadedEvent(metadata.getId()));
        log.info("Document {} uploaded by user {}", saved.getId(), userId);
        return mapToDto(saved);
    }

    private Document findDocumentWithOwnershipValidation(UUID documentId, UUID userId) {
        return repository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    private DocumentResponseDTO mapToDto(Document metadata) {
        return DocumentResponseDTO.builder()
                .id(metadata.getId())
                .fileName(metadata.getFileName())
                .contentType(metadata.getContentType())
                .size(metadata.getSize())
                .status(metadata.getStatus())
                .storagePath(metadata.getStoragePath())
                .thumbnailPath(metadata.getThumbnailPath())
                .createdTimestamp(metadata.getCreatedTimestamp())
                .build();
    }
}