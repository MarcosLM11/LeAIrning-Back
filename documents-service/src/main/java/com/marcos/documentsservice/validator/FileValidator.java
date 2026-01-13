package com.marcos.documentsservice.validator;

import com.marcos.documentsservice.exception.InvalidFileTypeException;
import com.marcos.documentsservice.exception.UnsupportedMediaTypeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class FileValidator {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/markdown"
    );

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB soft limit

    private final Tika tika = new Tika();

    public void validate(MultipartFile file) {
        // 1. Check for null file
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        // 2. Check for empty file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // 3. Check content type
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new UnsupportedMediaTypeException("Content type cannot be null");
        }

        // 4. Check if MIME type is allowed
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new UnsupportedMediaTypeException("Unsupported file type: " + contentType);
        }

        // 5. Verify actual file type using magic numbers (Tika)
        try {
            String detectedType = tika.detect(file.getInputStream());

            // For text files, Tika might detect as text/plain even for CSV/markdown
            // We need to be more lenient with text-based formats
            if (!isCompatibleType(contentType, detectedType)) {
                throw new InvalidFileTypeException(
                        "File content does not match declared type. Declared: " + contentType + ", Detected: " + detectedType
                );
            }
        } catch (IOException e) {
            throw new InvalidFileTypeException("Failed to detect file type: " + e.getMessage());
        }

        // 6. Check file size (soft limit - only log warning)
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Large file detected: {} bytes ({})", file.getSize(), file.getOriginalFilename());
        }
    }

    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Replace special characters with underscore, keeping only alphanumeric, dots, hyphens, and underscores
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean isCompatibleType(String declaredType, String detectedType) {
        // Exact match
        if (declaredType.equals(detectedType)) {
            return true;
        }

        // PDF must match exactly
        if (declaredType.equals("application/pdf")) {
            return detectedType.equals("application/pdf");
        }

        // DOC must match exactly
        if (declaredType.equals("application/msword")) {
            return detectedType.equals("application/msword") ||
                   detectedType.equals("application/x-tika-msoffice");
        }

        // DOCX must match (ZIP-based)
        if (declaredType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return detectedType.contains("openxmlformats") ||
                   detectedType.equals("application/zip") ||
                   detectedType.equals("application/x-tika-ooxml");
        }

        // Text-based formats are more flexible
        // CSV, plain text, and markdown can all be detected as text/plain
        if (declaredType.equals("text/plain") ||
            declaredType.equals("text/csv") ||
            declaredType.equals("text/markdown")) {
            return detectedType.equals("text/plain") ||
                   detectedType.equals("text/csv") ||
                   detectedType.equals("text/markdown") ||
                   detectedType.equals("text/x-web-markdown");
        }

        return false;
    }
}