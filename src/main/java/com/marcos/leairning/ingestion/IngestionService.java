package com.marcos.leairning.ingestion;

import com.marcos.leairning.documents.DocumentsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class IngestionService {

    private final DocumentsRepository repository;
    private final VectorStore vectorStore;

    public IngestionService(DocumentsRepository repository,  VectorStore vectorStore) {
        this.repository = repository;
        this.vectorStore = vectorStore;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void ingest(IngestEvent event) {

        var splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .build();

        var entity = repository.findById(event.documentId());
        if (entity.isEmpty()) return;
        var reader = new TikaDocumentReader(new ByteArrayResource(entity.get().getContent()));
        var documents = reader.read().stream()
                .peek(document -> {
                    document.getMetadata().put("filename",entity.get().getFileName());
                    document.getMetadata().put("contentType",entity.get().getContentType());
                    document.getMetadata().put("userId",entity.get().getUserId());
                }).toList();
        var chunks = splitter.apply(documents);
        vectorStore.add(chunks);
        log.info("Loaded {} documents as {} chunks.", documents.size(), chunks.size());
    }
}