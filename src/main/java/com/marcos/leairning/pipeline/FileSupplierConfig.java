package com.marcos.leairning.pipeline;

import com.marcos.leairning.minio.MinioProcessingPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Configuration
public class FileSupplierConfig {

    private static final Logger log = LoggerFactory.getLogger(FileSupplierConfig.class);

    private final MinioProcessingPipelineService pipelineService;

    public FileSupplierConfig(MinioProcessingPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @Bean
    public Supplier<Flux<DocumentContext>> fileSupplier() {
        return () -> Flux.interval(Duration.ofSeconds(5))
                .flatMap(_ -> {
                    var pendingFiles = pipelineService.listPendingFiles();
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
            log.atInfo().log("Found file to process: {}", filePath);
            var documentId = extractDocumentId(filePath);
            var fileBytes = pipelineService.loadFromProcessing(filePath);
            pipelineService.markProcessed(filePath, true);
            log.atInfo().log("File processed successfully: {} (documentId={})", filePath, documentId);
            return Flux.just(DocumentContext.of(fileBytes, documentId));
        } catch (Exception e) {
            log.warn("Error processing file: {}", filePath);
            try {
                pipelineService.markProcessed(filePath, false);
            } catch (Exception ex) {
                log.warn("Failed to mark file as failed: {}", filePath);
            }
            return Flux.empty();
        }
    }

    /**
     * Extracts documentId from the processing path.
     * Expected format: pending/{documentId}_{filename}
     */
    private UUID extractDocumentId(String filePath) {
        var filename = filePath.substring(filePath.lastIndexOf('/') + 1);
        var underscoreIndex = filename.indexOf('_');
        if (underscoreIndex > 0) {
            var idPart = filename.substring(0, underscoreIndex);
            return UUID.fromString(idPart);
        }
        throw new IllegalArgumentException("Cannot extract documentId from path: " + filePath);
    }
}
