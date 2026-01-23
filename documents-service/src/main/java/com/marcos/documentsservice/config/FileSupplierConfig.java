package com.marcos.documentsservice.config;

import com.marcos.documentsservice.pipeline.DocumentContext;
import com.marcos.documentsservice.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class FileSupplierConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSupplierConfig.class);

    private final StorageService storageService;

    public FileSupplierConfig(StorageService storageService) {
        this.storageService = storageService;
    }

    @Bean
    public Supplier<Flux<DocumentContext>> fileSupplier() {
        return () -> Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> {
                    var pendingFiles = storageService.listPendingFiles();
                    if (pendingFiles.isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(pendingFiles)
                            .filter(this::isSupportedFile)
                            .flatMap(this::processFile);
                });
    }

    private boolean isSupportedFile(String path) {
        var name = path.toLowerCase();
        return name.endsWith(".pdf") || name.endsWith(".docx")
            || name.endsWith(".doc") || name.endsWith(".txt")
            || name.endsWith(".csv") || name.endsWith(".md");
    }

    private Flux<DocumentContext> processFile(String filePath) {
        try {
            LOGGER.info("Found file to process: {}", filePath);
            var documentId = extractDocumentId(filePath);
            var fileBytes = storageService.loadFromProcessing(filePath);
            storageService.markProcessed(filePath, true);
            LOGGER.info("File processed successfully: {} (documentId={})", filePath, documentId);
            return Flux.just(DocumentContext.of(fileBytes, documentId));
        } catch (Exception e) {
            LOGGER.error("Error processing file: {}", filePath, e);
            try {
                storageService.markProcessed(filePath, false);
            } catch (Exception ex) {
                LOGGER.error("Failed to mark file as failed: {}", filePath, ex);
            }
            return Flux.empty();
        }
    }

    /**
     * Extracts documentId from the processing path.
     * Expected format: pending/{documentId}_{filename}
     */
    private Long extractDocumentId(String filePath) {
        var filename = filePath.substring(filePath.lastIndexOf('/') + 1);
        var underscoreIndex = filename.indexOf('_');
        if (underscoreIndex > 0) {
            var idPart = filename.substring(0, underscoreIndex);
            return Long.parseLong(idPart);
        }
        throw new IllegalArgumentException("Cannot extract documentId from path: " + filePath);
    }
}