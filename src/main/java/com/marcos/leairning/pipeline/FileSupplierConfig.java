package com.marcos.leairning.pipeline;

import com.marcos.leairning.minio.MinioProcessingPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Flogger
@Configuration
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class FileSupplierConfig {

    MinioProcessingPipelineService pipelineService;

    @Bean
    public Supplier<Flux<DocumentContext>> fileSupplier() {
        return () -> Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> {
                    val pendingFiles = pipelineService.listPendingFiles();
                    if (pendingFiles.isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(pendingFiles)
                            .filter(this::isSupportedFile)
                            .flatMap(this::processFile);
                });
    }

    private boolean isSupportedFile(String path) {
        val name = path.toLowerCase();
        return name.endsWith(".pdf") || name.endsWith(".docx")
                || name.endsWith(".doc") || name.endsWith(".txt")
                || name.endsWith(".csv") || name.endsWith(".md");
    }

    private Flux<DocumentContext> processFile(String filePath) {
        try {
            log.atInfo().log("Found file to process: %s", filePath);
            val documentId = extractDocumentId(filePath);
            val fileBytes = pipelineService.loadFromProcessing(filePath);
            pipelineService.markProcessed(filePath, true);
            log.atInfo().log("File processed successfully: %s (documentId=%s)", filePath, documentId);
            return Flux.just(DocumentContext.of(fileBytes, documentId));
        } catch (Exception e) {
            log.atWarning().withCause(e).log("Error processing file: %s", filePath);
            try {
                pipelineService.markProcessed(filePath, false);
            } catch (Exception ex) {
                log.atWarning().withCause(ex).log("Failed to mark file as failed: %s", filePath);
            }
            return Flux.empty();
        }
    }

    /**
     * Extracts documentId from the processing path.
     * Expected format: pending/{documentId}_{filename}
     */
    private UUID extractDocumentId(String filePath) {
        val filename = filePath.substring(filePath.lastIndexOf('/') + 1);
        val underscoreIndex = filename.indexOf('_');
        if (underscoreIndex > 0) {
            val idPart = filename.substring(0, underscoreIndex);
            return UUID.fromString(idPart);
        }
        throw new IllegalArgumentException("Cannot extract documentId from path: " + filePath);
    }
}
