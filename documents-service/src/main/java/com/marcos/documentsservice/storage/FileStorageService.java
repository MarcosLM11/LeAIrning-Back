package com.marcos.documentsservice.storage;

import com.marcos.documentsservice.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "file", matchIfMissing = true)
public class FileStorageService implements StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);
    private static final String PENDING_DIR = "pending";
    private static final String PROCESSED_DIR = "processed";
    private static final String FAILED_DIR = "failed";

    private final String storageRootPath;
    private final String processingPath;

    public FileStorageService(
            @Value("${document.storage.location:./uploads}") String storageRootPath,
            @Value("${document.storage.processing-input:./storage/processing-input}") String processingPath) {
        this.storageRootPath = storageRootPath;
        this.processingPath = processingPath;
    }

    public String store(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }
        try {
            return store(file.getBytes(), file.getOriginalFilename(), file.getContentType(), userId);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read file content", e);
        }
    }

    @Override
    public String store(byte[] content, String filename, String contentType, Long userId) {
        if (content == null || content.length == 0) {
            throw new StorageException("Cannot store empty content");
        }
        try {
            var sanitizedFilename = sanitizeFilename(filename);
            var extension = getExtension(sanitizedFilename);
            var uniqueFilename = UUID.randomUUID() + extension;
            var now = LocalDate.now();
            var directoryPath = Paths.get(
                    storageRootPath,
                    userId.toString(),
                    String.valueOf(now.getYear()),
                    String.format("%02d", now.getMonthValue())
            );
            Files.createDirectories(directoryPath);
            var filePath = directoryPath.resolve(uniqueFilename);
            if (!filePath.normalize().startsWith(Paths.get(storageRootPath).normalize())) {
                throw new StorageException("Invalid storage path");
            }
            Files.write(filePath, content);
            LOGGER.info("Stored document: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            LOGGER.error("Failed to store document", e);
            throw new StorageException("Failed to store document", e);
        }
    }

    @Override
    public byte[] load(String storagePath) {
        try {
            var filePath = Paths.get(storagePath);
            if (!Files.exists(filePath)) {
                throw new FileStorageException("File not found: " + storagePath);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to load document: {}", storagePath, e);
            throw new StorageException("Failed to load document", e);
        }
    }

    @Override
    public InputStream loadAsStream(String storagePath) {
        try {
            var filePath = Paths.get(storagePath);
            if (!Files.exists(filePath)) {
                throw new StorageException("File not found: " + storagePath);
            }
            return new ByteArrayInputStream(Files.readAllBytes(filePath));
        } catch (IOException e) {
            LOGGER.error("Failed to load document stream: {}", storagePath, e);
            throw new StorageException("Failed to load document stream", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            var filePath = Paths.get(storagePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                LOGGER.info("Deleted document: {}", storagePath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to delete document: {}", storagePath, e);
            throw new StorageException("Failed to delete document", e);
        }
    }

    @Override
    public String copyToProcessing(String storagePath, Long documentId) {
        try {
            var sourceFile = Paths.get(storagePath);
            if (!Files.exists(sourceFile)) {
                throw new StorageException("Source file not found: " + storagePath);
            }
            var filename = sourceFile.getFileName().toString();
            var pendingDir = Paths.get(processingPath, PENDING_DIR);
            Files.createDirectories(pendingDir);
            var processingFilePath = pendingDir.resolve(documentId + "_" + filename);
            Files.copy(sourceFile, processingFilePath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Copied to processing: {}", processingFilePath);
            return processingFilePath.toString();
        } catch (IOException e) {
            LOGGER.error("Failed to copy document to processing", e);
            throw new StorageException("Failed to copy to processing", e);
        }
    }

    @Override
    public List<String> listPendingFiles() {
        var pendingFiles = new ArrayList<String>();
        var pendingDir = Paths.get(processingPath, PENDING_DIR);
        if (!Files.exists(pendingDir)) {
            return pendingFiles;
        }
        try (Stream<Path> files = Files.list(pendingDir)) {
            files.filter(Files::isRegularFile)
                 .map(Path::toString)
                 .forEach(pendingFiles::add);
        } catch (IOException e) {
            LOGGER.error("Failed to list pending files", e);
            throw new StorageException("Failed to list pending files", e);
        }
        return pendingFiles;
    }

    @Override
    public byte[] loadFromProcessing(String processingPath) {
        try {
            var filePath = Paths.get(processingPath);
            if (!Files.exists(filePath)) {
                throw new StorageException("Processing file not found: " + processingPath);
            }
            return Files.readAllBytes(filePath);
        } catch (StorageException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.error("Failed to load from processing: {}", processingPath, e);
            throw new StorageException("Failed to load from processing", e);
        }
    }

    @Override
    public void markProcessed(String processingPath, boolean success) {
        try {
            var sourcePath = Paths.get(processingPath);
            if (!Files.exists(sourcePath)) {
                LOGGER.warn("Processing file not found: {}", processingPath);
                return;
            }
            var filename = sourcePath.getFileName().toString();
            var targetDir = success ? PROCESSED_DIR : FAILED_DIR;
            var targetPath = sourcePath.getParent().getParent().resolve(targetDir);
            Files.createDirectories(targetPath);
            var targetFile = targetPath.resolve(filename);
            Files.move(sourcePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Marked as {}: {}", success ? "processed" : "failed", targetFile);
        } catch (IOException e) {
            LOGGER.error("Failed to mark document as processed", e);
            throw new StorageException("Failed to mark as processed", e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }
        var cleaned = filename.replace("../", "");
        return cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getExtension(String filename) {
        var lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}