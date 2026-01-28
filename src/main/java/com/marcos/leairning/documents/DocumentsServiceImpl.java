package com.marcos.leairning.documents;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

    @Override
    public Page<DocumentResponseDTO> getDocuments(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDTO);
    }

    @Override
    public void upload(List<MultipartFile> files) {
        files.forEach(this::uploadDocument);
    }

    @Override
    public DocumentResponseDTO getDocument(UUID id) {
        val document = repository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Unable to find document with id: " + id)
        );

        return mapper.toDTO(document);
    }

    @Override
    public void deleteDocument(UUID id) {
        if (!repository.existsById(id)) {

            throw new IllegalArgumentException("Unable to find document with id: " + id);

        }

        repository.deleteById(id);
    }


    private void uploadDocument(MultipartFile file) {
        validateDocument(file);
        val document = mapper.toEntity(file);
        document.setUserId(UUID.randomUUID());
        document.setFileName(sanitizeFilename(file.getOriginalFilename()));
        repository.save(document);
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
