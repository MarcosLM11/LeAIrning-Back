package com.marcos.documentsservice.service;

import com.marcos.documentsservice.entity.Document;
import com.marcos.documentsservice.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class DocumentProcessorServiceImpl implements DocumentProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentProcessorServiceImpl.class);

    private final FileStorageService fileStorageService;

    @Value("${document.storage.processing-input:./storage/processing-input}")
    private String processingInputDirectory;

    @Override
    @Async
    public void processDocumentAsync(Document document) {
        try {
            // Create processing directory if it doesn't exist
            Path processingDir = Path.of(processingInputDirectory);
            Files.createDirectories(processingDir);

            // Load file from storage
            byte[] fileBytes = fileStorageService.load(document.getStoragePath());

            // Copy to processing directory where pipeline monitors
            Path targetPath = processingDir.resolve(document.getStoredFilename());
            Files.write(targetPath, fileBytes);
        } catch (IOException e) {
            LOGGER.error("Error copying document to pipeline directory: {}", document.getId(), e);
        }
    }
}