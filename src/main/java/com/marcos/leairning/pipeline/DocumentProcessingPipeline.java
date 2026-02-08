package com.marcos.leairning.pipeline;

import com.marcos.leairning.documents.DocumentsRepository;
import com.marcos.leairning.exception.DocumentReaderException;
import com.marcos.leairning.exception.VectorStoreException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reactive pipeline for RAG document processing.
 * Pipeline flow:
 * 1. fileSupplier: Reads file bytes and extracts documentId (configured externally)
 * 2. metadataEnricher: Loads document entity from database to get userId
 * 3. documentReader: Parses with TikaDocumentReader and adds metadata
 * 4. splitter: Splits into chunks with TokenTextSplitter
 * 5. vectorStoreConsumer: Stores in Qdrant with embeddings
 */
@Flogger
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentProcessingPipeline {

    PipelineProperties properties;
    DocumentsRepository repository;

    /**
     * Step 1: Enriches DocumentContext with full document entity from database.
     * This allows us to access userId and other metadata for vector store filtering.
     *
     * @return Function that enriches DocumentContext with document entity
     */
    @Bean
    public Function<Flux<DocumentContext>, Flux<DocumentContext>> metadataEnricher() {
        return contextFlux -> contextFlux
                .doOnNext(ctx -> log.atInfo().log("Enriching metadata for documentId=%s", ctx.documentId()))
                .map(ctx -> {
                    val document = repository.findById(ctx.documentId())
                            .orElseThrow(() -> new IllegalStateException("Document not found: " + ctx.documentId()));
                    log.atInfo().log("Loaded document metadata: id=%s, userId=%s, filename=%s",
                            document.getId(), document.getUserId(), document.getFileName());
                    return ctx.withDocument(document);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Step 2: Reads file and parses with TikaDocumentReader, adding metadata.
     *
     * @return Function that transforms DocumentContext into Spring AI Document with metadata
     */
    @Bean
    public Function<Flux<DocumentContext>, Flux<Document>> documentReader() {
        return contextFlux -> contextFlux
                .doOnNext(ctx -> log.atInfo().log("Reading document (%d bytes)", ctx.fileBytes().length))
                .flatMapIterable(ctx -> {
                    try {
                        var documents = new TikaDocumentReader(new ByteArrayResource(ctx.fileBytes())).get();

                        if (documents.isEmpty()) {
                            log.atWarning().log("TikaDocumentReader returned empty document list");
                            throw new DocumentReaderException("Failed to extract text from document");
                        }

                        documents.forEach(doc -> addMetadata(doc, ctx));

                        log.atInfo().log("Extracted %d documents with metadata: userId=%s, documentId=%s", documents.size(), ctx.document().getUserId(), ctx.documentId());
                        return documents;
                    } catch (DocumentReaderException e) {
                        throw e;
                    } catch (Exception e) {
                        log.atWarning().withCause(e).log("Error reading document with Tika");
                        throw new DocumentReaderException("Document reading failed", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void addMetadata(Document doc, DocumentContext ctx) {
        var entity = ctx.document();
        doc.getMetadata().put("userId", entity.getUserId().toString());
        doc.getMetadata().put("documentId", entity.getId().toString());
        doc.getMetadata().put("documentTitle", entity.getFileName());
        doc.getMetadata().put("contentType", entity.getContentType());
        doc.getMetadata().put("documentType", entity.getFileName().split("\\.")[1]);
    }

    private TextSplitter createDefaultTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(properties.getChunkSize())
                .withKeepSeparator(properties.getKeepSeparator())
                .withMinChunkLengthToEmbed(properties.getMinChunkLenght())
                .build();
    }

    /**
     * Step 3: Splits a document into chunks using TokenTextSplitter.
     * Spring AI automatically preserves metadata across chunks.
     *
     * @return Function that transforms Document into List of Document chunks
     */
    @Bean
    public Function<Flux<Document>, Flux<List<Document>>> splitter() {
        return splitter(createDefaultTextSplitter());
    }

    /**
     * Step 3: Splits a document into chunks using a custom TextSplitter.
     *
     * @param textSplitter Custom TextSplitter implementation
     * @return Function that transforms Document into List of Document chunks
     */
    public Function<Flux<Document>, Flux<List<Document>>> splitter(TextSplitter textSplitter) {
        return documentFlux -> documentFlux
                .doOnNext(doc -> log.atInfo().log("Splitting document into chunks (chunk size: %d)", properties.getChunkSize()))
                .map(incoming -> {
                    List<Document> chunks = textSplitter.apply(List.of(incoming));
                    log.atInfo().log("Document split into %d chunks with metadata preserved", chunks.size());
                    return chunks;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Step 4: Stores chunks in Qdrant with embeddings.
     * Metadata (userId, documentId, etc.) is stored alongside vectors for filtering.
     *
     * @param vectorStore VectorStore from Spring AI (configured in application.yml)
     * @return Consumer that writes to Qdrant
     */
    @Bean
    public Consumer<Flux<List<Document>>> vectorStoreConsumer(VectorStore vectorStore) {
        return documentFlux -> documentFlux
                .flatMap(documents -> Mono.fromCallable(() -> {
                    if (!documents.isEmpty()) {
                        var docCount = documents.size();
                        log.atInfo().log("Writing %d document chunks to vector store", docCount);
                        try {
                            vectorStore.accept(documents);
                            log.atInfo().log("%d document chunks written to vector store successfully", docCount);
                        } catch (Exception e) {
                            log.atWarning().withCause(e).log("Error writing to vector store");
                            throw new VectorStoreException("Vector store write failed", e);
                        }
                    } else {
                        log.atWarning().log("Empty document list, skipping vector store write");
                    }
                    return documents;
                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnError(error -> log.atWarning().withCause(error).log("Pipeline error occurred"))
                .doOnComplete(() -> log.atInfo().log("Pipeline completed successfully"))
                .subscribe();
    }

}
