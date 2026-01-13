package com.marcos.documentsservice.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reactive pipeline for RAG document processing.
 *
 * Pipeline flow:
 * 1. fileSupplier: Reads file bytes (configured externally)
 * 2. documentReader: Parses with TikaDocumentReader
 * 3. splitter: Splits into chunks with TokenTextSplitter
 * 4. vectorStoreConsumer: Stores in Qdrant with embeddings
 */
@Component
public class DocumentProcessingPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentProcessingPipeline.class);

    @Value("${document.processing.chunk-size:500}")
    private int chunkSize;

    @Value("${document.processing.keep-separator:false}")
    private boolean keepSeparator;

    @Value("${document.processing.min-chunk-length:10}")
    private int minChunkLength;

    /**
     * Step 1: Reads file and parses with TikaDocumentReader.
     *
     * @return Function that transforms bytes into Spring AI Document
     */
    @Bean
    public Function<Flux<byte[]>, Flux<Document>> documentReader() {
        return resourceFlux -> resourceFlux
                .doOnNext(bytes -> LOGGER.info("Received file for processing ({} bytes)", bytes.length))
                .map(fileBytes -> {
                    try {
                        // TikaDocumentReader extracts text from PDFs, DOCX, TXT, etc.
                        List<Document> documents = new TikaDocumentReader(new ByteArrayResource(fileBytes))
                                .get();

                        if (documents.isEmpty()) {
                            LOGGER.warn("TikaDocumentReader returned empty document list");
                            throw new RuntimeException("Failed to extract text from document");
                        }

                        Document doc = documents.getFirst();
                        LOGGER.info("Document extracted successfully. Text length: {}", doc.getText().length());
                        return doc;
                    } catch (Exception e) {
                        LOGGER.error("Error reading document with Tika", e);
                        throw new RuntimeException("Document reading failed", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates the default TextSplitter (TokenTextSplitter) for production use.
     *
     * @return TextSplitter configured with application properties
     */
    private TextSplitter createDefaultTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withKeepSeparator(keepSeparator)
                .withMinChunkLengthToEmbed(minChunkLength)
                .build();
    }

    /**
     * Step 2: Splits document into chunks using TokenTextSplitter.
     *
     * @return Function that transforms Document into List of Document chunks
     */
    @Bean
    public Function<Flux<Document>, Flux<List<Document>>> splitter() {
        return splitter(createDefaultTextSplitter());
    }

    /**
     * Step 2: Splits document into chunks using a custom TextSplitter.
     * This method is useful for testing with a simple splitter that doesn't require external services.
     *
     * @param textSplitter Custom TextSplitter implementation
     * @return Function that transforms Document into List of Document chunks
     */
    public Function<Flux<Document>, Flux<List<Document>>> splitter(TextSplitter textSplitter) {
        return documentFlux -> documentFlux
                .doOnNext(doc -> LOGGER.info("Splitting document into chunks (chunk size: {})", chunkSize))
                .map(incoming -> {
                    List<Document> chunks = textSplitter.apply(List.of(incoming));
                    LOGGER.info("Document split into {} chunks", chunks.size());
                    return chunks;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Step 3: Stores chunks in Qdrant with embeddings.
     *
     * @param vectorStore VectorStore from Spring AI (configured in application.yml)
     * @return Consumer that writes to Qdrant
     */
    @Bean
    public Consumer<Flux<List<Document>>> vectorStoreConsumer(VectorStore vectorStore) {
        return documentFlux -> documentFlux
                .doOnNext(documents -> {
                    if (!documents.isEmpty()) {
                        var docCount = documents.size();
                        LOGGER.info("Writing {} document chunks to vector store", docCount);

                        try {
                            // VectorStore automatically generates embeddings with Ollama
                            vectorStore.accept(documents);
                            LOGGER.info("{} document chunks written to vector store successfully", docCount);
                        } catch (Exception e) {
                            LOGGER.error("Error writing to vector store", e);
                            throw new RuntimeException("Vector store write failed", e);
                        }
                    } else {
                        LOGGER.warn("Empty document list, skipping vector store write");
                    }
                })
                .doOnError(error -> LOGGER.error("Pipeline error occurred", error))
                .doOnComplete(() -> LOGGER.info("Pipeline completed successfully"))
                .subscribe();
    }
}