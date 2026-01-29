package com.marcos.leairning.documents;

import com.giffing.bucket4j.spring.boot.starter.context.IgnoreRateLimiting;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import com.marcos.leairning.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
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
    MinioService minioService;

    @Override
    public Page<DocumentResponseDTO> getDocuments(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @SneakyThrows
    @Transactional
    public List<DocumentResponseDTO> upload(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadDocument)
                .toList();
    }

    @Override
    @IgnoreRateLimiting
    @Cacheable(value = "documents", key = "#id")
    public DocumentResponseDTO getDocument(UUID id) {
        val document = repository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Unable to find document with id: " + id)
        );

        return mapper.toDTO(document);
    }

    @Override
    @Transactional
    @CacheEvict(value = "documents", key = "#id")
    public void deleteDocument(UUID id) {
        val document = repository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Unable to find document with id: " + id)
        );

        minioService.delete(document.getStoragePath());
        repository.deleteById(id);
    }


    private DocumentResponseDTO uploadDocument(MultipartFile file) {
        validateDocument(file);

        val document = mapper.toEntity(file);
        document.setUserId(UUID.randomUUID());
        document.setFileName(sanitizeFilename(file.getOriginalFilename()));

        try {
            val objectPath = minioService.store(file.getBytes(), document);
            document.setStoragePath(objectPath);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }

        val saved = repository.save(document);
        return mapper.toDTO(saved);
    }

    public byte[] downloadDocument(UUID documentId, UUID userId) {
        val document = repository.findById(documentId).orElseThrow(
                () -> new IllegalArgumentException("Unable to find document with id: " + documentId)
        );

        if (!document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User does not have access to this document");
        }

        return minioService.load(document.getStoragePath());
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
