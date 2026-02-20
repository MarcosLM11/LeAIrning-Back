package com.marcos.leairning.documents;

import com.giffing.bucket4j.spring.boot.starter.context.IgnoreRateLimiting;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import com.marcos.leairning.exception.DocumentAccessDeniedException;
import com.marcos.leairning.exception.DocumentNotFoundException;
import com.marcos.leairning.exception.DocumentProcessingException;
import com.marcos.leairning.minio.MinioDocumentStorageService;
import com.marcos.leairning.minio.MinioProcessingPipelineService;
import org.apache.tika.Tika;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static com.marcos.leairning.cache.CaffeineCacheProperties.DEFAULT_POLICY;

@Flogger
@Service
@RequiredArgsConstructor
@RateLimiting(name = DEFAULT_POLICY)
@Transactional(readOnly = true)
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class DocumentsServiceImpl implements DocumentsService {

    private static final String PDF = "application/pdf";
    private static final String DOC = "application/msword";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String TXT = "text/plain";
    private static final String CSV = "text/csv";
    private static final String MD = "text/markdown";

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            PDF, DOC, DOCX, TXT, CSV, MD
    );
    private static final long MAX_DECOMPRESSION_RATIO = 100;
    private static final Tika TIKA = new Tika();

    DocumentsRepository repository;
    DocumentsMapper mapper;
    MinioDocumentStorageService storageService;
    MinioProcessingPipelineService pipelineService;

    @Override
    public Page<DocumentResponseDTO> getDocuments(UUID userId, Pageable pageable) {
        log.atFine().log("Fetching documents for user %s, page: %d, size: %d", userId, pageable.getPageNumber(), pageable.getPageSize());
        return repository.findByUserId(userId, pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional
    public List<DocumentResponseDTO> upload(UUID userId, List<MultipartFile> files) {
        log.atInfo().log("User %s uploading %d documents", userId, files.size());
        val result = files.stream()
                .map(file -> uploadDocument(userId, file))
                .toList();

        log.atInfo().log("Successfully uploaded %d documents for user %s", result.size(), userId);
        return result;
    }

    @Override
    @IgnoreRateLimiting
    @Cacheable(value = "documents", key = "#userId + '-' + #documentId")
    public DocumentResponseDTO getDocument(UUID userId, UUID documentId) {
        log.atFine().log("Fetching document %s for user %s", documentId, userId);
        return mapper.toDTO(findDocumentWithOwnershipValidation(documentId, userId));
    }

    @Override
    @Transactional
    @CacheEvict(value = "documents", key = "#userId + '-' + #documentId")
    public void deleteDocument(UUID userId, UUID documentId) {
        log.atInfo().log("User %s deleting document %s", userId, documentId);
        val document = findDocumentWithOwnershipValidation(documentId, userId);
        storageService.delete(document.getStoragePath());
        repository.deleteById(documentId);
        log.atInfo().log("Document %s deleted successfully by user %s", documentId, userId);
    }

    @Override
    public byte[] downloadDocument(UUID userId, UUID documentId) {
        log.atFine().log("User %s downloading document %s", userId, documentId);
        val document = findDocumentWithOwnershipValidation(documentId, userId);
        return storageService.load(document.getStoragePath());
    }

    @Override
    @Transactional
    public void deleteDocuments(UUID userId, List<UUID> documentIds) {
        log.atInfo().log("User %s batch deleting %d documents", userId, documentIds.size());
        
        val documentsToDelete = repository.findByIdInAndUserId(documentIds, userId);
        
        if (documentsToDelete.isEmpty()) {
            log.atFine().log("No documents found to delete for user %s", userId);
            return;
        }

        documentsToDelete.forEach(doc -> storageService.delete(doc.getStoragePath()));
        
        val idsToDelete = documentsToDelete.stream()
                .map(Document::getId)
                .toList();
        
        int deletedCount = repository.deleteByIdInAndUserId(idsToDelete, userId);
        log.atInfo().log("Batch deleted %d documents for user %s", deletedCount, userId);
    }

    private DocumentResponseDTO uploadDocument(UUID userId, MultipartFile file) {
        validateDocument(file);
        validateFileContent(file);
        val document = mapper.toEntity(file);
        document.setUserId(userId);
        document.setFileName(sanitizeFilename(file.getOriginalFilename()));
        
        try {
            val objectPath = storageService.store(file.getBytes(), document);
            document.setStoragePath(objectPath);
            
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read file bytes", e);
        }
        
        val saved = repository.save(document);
        pipelineService.copyToProcessing(saved.getStoragePath(), saved.getId());
        log.atFine().log("Document %s uploaded by user %s", saved.getId(), userId);
        return mapper.toDTO(saved);
    }

    private Document findDocumentWithOwnershipValidation(UUID documentId, UUID userId) {
        return repository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> {
                    if (repository.existsById(documentId)) {
                        return new DocumentAccessDeniedException(documentId, userId);
                    }
                    return new DocumentNotFoundException(documentId);
                });
    }

    public void validateDocument(MultipartFile file) {

        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        val contentType = file.getContentType();
        if (contentType == null) {
            throw new UnsupportedMediaTypeException("Content type cannot be null");
        }

        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new UnsupportedMediaTypeException("Unsupported file type: " + contentType);
        }
    }

    void validateFileContent(MultipartFile file) {
        try {
            var detectedType = TIKA.detect(file.getInputStream());
            if (!isContentTypeCompatible(detectedType)) {
                throw new UnsupportedMediaTypeException(
                        "File content type mismatch: detected " + detectedType);
            }
            // Decompression ratio check for ZIP-based formats (DOCX, etc.)
            var compressedSize = file.getSize();
            if (compressedSize > 0 && "application/zip".equals(detectedType)) {
                var bytes = file.getBytes();
                if (bytes.length > compressedSize * MAX_DECOMPRESSION_RATIO) {
                    throw new UnsupportedMediaTypeException("File exceeds maximum decompression ratio");
                }
            }
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to validate file content", e);
        }
    }

    private boolean isContentTypeCompatible(String detectedType) {
        if (ALLOWED_MIME_TYPES.contains(detectedType)) {
            return true;
        }
        // Tika detects CSV, MD, and other text variants as text/plain
        return TXT.equals(detectedType);
    }

    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
