package com.marcos.leairning.documents;

import com.giffing.bucket4j.spring.boot.starter.context.IgnoreRateLimiting;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import com.marcos.leairning.exception.DocumentAccessDeniedException;
import com.marcos.leairning.exception.DocumentNotFoundException;
import com.marcos.leairning.exception.DocumentProcessingException;
import com.marcos.leairning.minio.MinioDocumentStorageService;
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

    DocumentsRepository repository;
    DocumentsMapper mapper;
    MinioDocumentStorageService storageService;

    @Override
    public Page<DocumentResponseDTO> getDocuments(Pageable pageable) {
        log.atFine().log("Fetching documents page: %d, size: %d", pageable.getPageNumber(), pageable.getPageSize());
        return repository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional
    public List<DocumentResponseDTO> upload(List<MultipartFile> files) {
        log.atInfo().log("Uploading %d documents", files.size());
        val result = files.stream()
                .map(this::uploadDocument)
                .toList();

        log.atInfo().log("Successfully uploaded %d documents", result.size());
        return result;
    }

    @Override
    @IgnoreRateLimiting
    @Cacheable(value = "documents", key = "#id")
    public DocumentResponseDTO getDocument(UUID id) {
        log.atFine().log("Fetching document with id: %s", id);
        return mapper.toDTO(findDocumentOrThrow(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "documents", key = "#id")
    public void deleteDocument(UUID id) {
        log.atInfo().log("Deleting document with id: %s", id);
        val document = findDocumentOrThrow(id);
        storageService.delete(document.getStoragePath());
        repository.deleteById(id);
        log.atInfo().log("Document deleted successfully: %s", id);
    }

    private DocumentResponseDTO uploadDocument(MultipartFile file) {
        validateDocument(file);
        val document = mapper.toEntity(file);
        document.setUserId(UUID.randomUUID());
        document.setFileName(sanitizeFilename(file.getOriginalFilename()));
        
        try {
            val objectPath = storageService.store(file.getBytes(), document);
            document.setStoragePath(objectPath);
        
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read file bytes", e);
        }
        
        val saved = repository.save(document);
        log.atFine().log("Document uploaded: %s", saved.getId());
        return mapper.toDTO(saved);
    }

    public byte[] downloadDocument(UUID documentId, UUID userId) {
        log.atFine().log("Downloading document %s for user %s", documentId, userId);
        val document = findDocumentOrThrow(documentId);
        
        if (!document.getUserId().equals(userId)) {
            throw new DocumentAccessDeniedException(documentId, userId);
        
        }
        return storageService.load(document.getStoragePath());
    }

    private Document findDocumentOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
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

    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
