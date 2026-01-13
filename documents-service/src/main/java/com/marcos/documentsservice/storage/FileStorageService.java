package com.marcos.documentsservice.storage;

import com.marcos.documentsservice.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String storageRootPath;

    public FileStorageService(@Value("${document.storage.root-path:./storage/documents}") String storageRootPath) {
        this.storageRootPath = storageRootPath;
    }

    public String store(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }

        try {
            // Sanitize and process filename
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = sanitizeFilename(originalFilename);

            // Extract extension
            String extension = "";
            if (sanitizedFilename.contains(".")) {
                extension = sanitizedFilename.substring(sanitizedFilename.lastIndexOf("."));
            }

            // Generate unique filename with UUID
            String uniqueFilename = UUID.randomUUID() + extension;

            // Build path: rootPath/userId/year/month/uniqueFilename
            LocalDate now = LocalDate.now();
            Path directoryPath = Paths.get(
                    storageRootPath,
                    userId.toString(),
                    String.valueOf(now.getYear()),
                    String.format("%02d", now.getMonthValue())
            );

            // Create directories if they don't exist
            Files.createDirectories(directoryPath);

            // Build complete file path
            Path filePath = directoryPath.resolve(uniqueFilename);

            // Validate path to prevent traversal attacks
            if (!filePath.normalize().startsWith(Paths.get(storageRootPath).normalize())) {
                throw new FileStorageException("Invalid storage path");
            }

            // Store file
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return filePath.toString();

        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }

    public byte[] load(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath);
            if (!Files.exists(filePath)) {
                throw new FileStorageException("File not found: " + storagePath);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to load file", e);
        }
    }

    public void delete(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            // Do not throw an exception if the file doesn't exist
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }
        // Remove path traversal attempts and sanitize
        String cleaned = filename.replace("\\.\\./", "");
        // Replace special characters with underscore, keeping only alphanumeric, dots, hyphens, and underscores
        return cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}