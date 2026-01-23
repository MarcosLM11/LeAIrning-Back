package com.marcos.documentsservice.pipeline;

import com.marcos.documentsservice.exception.DocumentReaderException;
import com.marcos.documentsservice.exception.VectorStoreException;
import com.marcos.documentsservice.repository.DocumentRepository;
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
 * Pipeline flow:
 * 1. fileSupplier: Reads file bytes and extracts documentId (configured externally)
 * 2. metadataEnricher: Loads document entity from database to get userId
 * 3. documentReader: Parses with TikaDocumentReader and adds metadata
 * 4. splitter: Splits into chunks with TokenTextSplitter
 * 5. vectorStoreConsumer: Stores in Qdrant with embeddings
 */
@Component
public class DocumentProcessingPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentProcessingPipeline.class);

    private final DocumentRepository documentRepository;

    @Value("${document.processing.chunk-size:500}")
    private int chunkSize;

    @Value("${document.processing.keep-separator:false}")
    private boolean keepSeparator;

    @Value("${document.processing.min-chunk-length:10}")
    private int minChunkLength;

    public DocumentProcessingPipeline(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Step 1: Enriches DocumentContext with full document entity from database.
     * This allows us to access userId and other metadata for vector store filtering.
     *
     * @return Function that enriches DocumentContext with document entity
     */
    @Bean
    public Function<Flux<DocumentContext>, Flux<DocumentContext>> metadataEnricher() {
        return contextFlux -> contextFlux
                .doOnNext(ctx -> LOGGER.info("Enriching metadata for documentId={}", ctx.documentId()))
                .map(ctx -> {
                    var document = documentRepository.findById(ctx.documentId())
                            .orElseThrow(() -> new DocumentReaderException(
                                    "Document not found: " + ctx.documentId()));
                    LOGGER.info("Loaded document metadata: id={}, userId={}, filename={}",
                            document.getId(), document.getUserId(), document.getOriginalFilename());
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
                .doOnNext(ctx -> LOGGER.info("Reading document ({} bytes)", ctx.fileBytes().length))
                .map(ctx -> {
                    try {
                        List<Document> documents = new TikaDocumentReader(
                                new ByteArrayResource(ctx.fileBytes())).get();
                        if (documents.isEmpty()) {
                            LOGGER.warn("TikaDocumentReader returned empty document list");
                            throw new DocumentReaderException("Failed to extract text from document");
                        }
                        Document doc = documents.getFirst();
                        addMetadata(doc, ctx);
                        LOGGER.info("Document extracted with metadata: userId={}, documentId={}",
                                ctx.document().getUserId(), ctx.documentId());
                        return doc;
                    } catch (DocumentReaderException e) {
                        throw e;
                    } catch (Exception e) {
                        LOGGER.error("Error reading document with Tika", e);
                        throw new DocumentReaderException("Document reading failed", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void addMetadata(Document doc, DocumentContext ctx) {
        var entity = ctx.document();
        doc.getMetadata().put("userId", entity.getUserId());
        doc.getMetadata().put("documentId", entity.getId());
        doc.getMetadata().put("documentTitle", entity.getOriginalFilename());
        doc.getMetadata().put("contentType", entity.getContentType());
        doc.getMetadata().put("documentType", entity.getDocumentType().name());
    }

    private TextSplitter createDefaultTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withKeepSeparator(keepSeparator)
                .withMinChunkLengthToEmbed(minChunkLength)
                .build();
    }

    /**
     * Step 3: Splits document into chunks using TokenTextSplitter.
     * Metadata is automatically preserved across chunks by Spring AI.
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
                .doOnNext(doc -> LOGGER.info("Splitting document into chunks (chunk size: {})", chunkSize))
                .map(incoming -> {
                    List<Document> chunks = textSplitter.apply(List.of(incoming));
                    LOGGER.info("Document split into {} chunks with metadata preserved", chunks.size());
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
                .doOnNext(documents -> {
                    if (!documents.isEmpty()) {
                        var docCount = documents.size();
                        LOGGER.info("Writing {} document chunks to vector store", docCount);
                        try {
                            vectorStore.accept(documents);
                            LOGGER.info("{} document chunks written to vector store successfully", docCount);
                        } catch (Exception e) {
                            LOGGER.error("Error writing to vector store", e);
                            throw new VectorStoreException("Vector store write failed", e);
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