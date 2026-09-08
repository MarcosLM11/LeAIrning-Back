package com.marcos.leairning.documents;

import com.giffing.bucket4j.spring.boot.starter.context.IgnoreRateLimiting;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import com.marcos.leairning.exception.DocumentNotFoundException;
import com.marcos.leairning.exception.DocumentProcessingException;
import com.marcos.leairning.minio.MinioDocumentStorageService;
import com.marcos.leairning.minio.MinioProcessingPipelineService;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;
import static com.marcos.leairning.cache.CaffeineCacheProperties.DEFAULT_POLICY;

@Service
@RateLimiting(name = DEFAULT_POLICY)
@Transactional(readOnly = true)
public class DocumentsServiceImpl implements DocumentsService {

    private static final Logger log = LoggerFactory.getLogger(DocumentsServiceImpl.class);
    private static final long MAX_DECOMPRESSION_RATIO = 100;
    private static final Tika TIKA = new Tika();

    private final DocumentsRepository repository;
    private final DocumentsMapper mapper;
    private final MinioDocumentStorageService storageService;
    private final MinioProcessingPipelineService pipelineService;

    public DocumentsServiceImpl(DocumentsRepository repository,  DocumentsMapper mapper, MinioDocumentStorageService storageService,  MinioProcessingPipelineService pipelineService) {
        this.repository = repository;
        this.mapper = mapper;
        this.storageService = storageService;
        this.pipelineService = pipelineService;
    }

    @Override
    public Page<DocumentResponseDTO> getDocuments(UUID userId, Pageable pageable) {
        log.info("Fetching documents for user {}, page: {}, size: {}", userId, pageable.getPageNumber(), pageable.getPageSize());
        return repository.findByUserId(userId, pageable).map(mapper::toDTO);
    }

    @Override
    @Transactional
    public List<DocumentResponseDTO> upload(UUID userId, List<MultipartFile> files) {
        log.info("User {} uploading {} documents", userId, files.size());
        var result = files.stream()
                .map(file -> uploadDocument(userId, file))
                .toList();

        log.info("Successfully uploaded {} documents for user {}", result.size(), userId);
        return result;
    }

    @Override
    @IgnoreRateLimiting
    @Cacheable(value = "documents", key = "#userId + '-' + #documentId")
    public DocumentResponseDTO getDocument(UUID userId, UUID documentId) {
        log.info("Fetching document {} for user {}", documentId, userId);
        return mapper.toDTO(findDocumentWithOwnershipValidation(documentId, userId));
    }

    @Override
    @Transactional
    @CacheEvict(value = "documents", key = "#userId + '-' + #documentId")
    public void deleteDocument(UUID userId, UUID documentId) {
        log.info("User {} deleting document {}", userId, documentId);
        var document = findDocumentWithOwnershipValidation(documentId, userId);
        storageService.delete(document.getStoragePath());
        repository.deleteById(documentId);
        log.atInfo().log("Document {} deleted successfully by user {}", documentId, userId);
    }

    @Override
    public byte[] downloadDocument(UUID userId, UUID documentId) {
        log.info("User {} downloading document {}", userId, documentId);
        var document = findDocumentWithOwnershipValidation(documentId, userId);
        return storageService.load(document.getStoragePath());
    }

    @Override
    @Transactional
    public void deleteDocuments(UUID userId, List<UUID> documentIds) {
        log.info("User {} batch deleting {} documents", userId, documentIds.size());
        var documentsToDelete = repository.findByIdInAndUserId(documentIds, userId);
        
        if (documentsToDelete.isEmpty()) return;

        documentsToDelete.forEach(doc -> storageService.delete(doc.getStoragePath()));
        
        var idsToDelete = documentsToDelete.stream()
                .map(Document::getId)
                .toList();
        
        int deletedCount = repository.deleteByIdInAndUserId(idsToDelete, userId);
        log.info("Batch deleted {} documents for user {}", deletedCount, userId);
    }

    private DocumentResponseDTO uploadDocument(UUID userId, MultipartFile file) {
        validateDocument(file);
        validateFileContent(file);
        var document = mapper.toEntity(file);
        document.setUserId(userId);
        document.setFileName(sanitizeFilename(file.getOriginalFilename()));
        
        try {
            var objectPath = storageService.store(file.getBytes(), document);
            document.setStoragePath(objectPath);
            
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read file bytes", e);
        }
        
        var saved = repository.save(document);
        pipelineService.copyToProcessing(saved.getStoragePath(), saved.getId());
        log.info("Document {} uploaded by user {}", saved.getId(), userId);
        return mapper.toDTO(saved);
    }

    private Document findDocumentWithOwnershipValidation(UUID documentId, UUID userId) {
        return repository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public void validateDocument(MultipartFile file) {

        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getContentType() == null) {
            throw new UnsupportedMediaTypeException("Content type cannot be null");
        }

        if (!MimeTypes.isValid(file.getContentType())) {
            throw new UnsupportedMediaTypeException("Unsupported file type: " + file.getContentType());
        }
    }

    void validateFileContent(MultipartFile file) {
        try {
            var detectedType = TIKA.detect(file.getInputStream());

            if (!MimeTypes.isValid(file.getContentType())) {
                throw new UnsupportedMediaTypeException("File content type mismatch: detected " + detectedType);
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

    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}