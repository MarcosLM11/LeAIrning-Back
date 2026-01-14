package com.marcos.documentsservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Configuration
public class FileSupplierConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSupplierConfig.class);

    @Value("${document.storage.processing-input:./storage/processing-input}")
    private String processingDirectory;

    @Bean
    public Supplier<Flux<byte[]>> fileSupplier() {
        return () -> Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> {
                    try {
                        Path dir = Paths.get(processingDirectory);
                        if (!Files.exists(dir)) {
                            Files.createDirectories(dir);
                            return Flux.empty();
                        }

                        try (Stream<Path> files = Files.list(dir)) {
                            return Flux.fromIterable(files
                                    .filter(Files::isRegularFile)
                                    .filter(path -> {
                                        String name = path.getFileName().toString().toLowerCase();
                                        return name.endsWith(".pdf") || name.endsWith(".docx")
                                            || name.endsWith(".doc") || name.endsWith(".txt")
                                            || name.endsWith(".csv") || name.endsWith(".md");
                                    })
                                    .toList())
                                    .flatMap(this::processFile);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error reading processing directory", e);
                        return Flux.empty();
                    }
                });
    }

    private Flux<byte[]> processFile(Path filePath) {
        try {
            LOGGER.info("Found file to process: {}", filePath.getFileName());
            byte[] fileBytes = Files.readAllBytes(filePath);
            Files.delete(filePath);
            LOGGER.info("File read and removed from processing directory: {}", filePath.getFileName());
            return Flux.just(fileBytes);
        } catch (IOException e) {
            LOGGER.error("Error processing file: {}", filePath, e);
            return Flux.empty();
        }
    }
}