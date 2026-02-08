package com.marcos.leairning.pipeline;

import com.marcos.leairning.documents.DocumentsRepository;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DocumentProcessingPipelineTest {

    DocumentProcessingPipeline pipeline;
    DocumentsRepository repository;
    PipelineProperties properties;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentsRepository.class);
        properties = new PipelineProperties();
        properties.setChunkSize(800);
        properties.setKeepSeparator(true);
        properties.setMinChunkLenght(50);
        pipeline = new DocumentProcessingPipeline(properties, repository);
    }

    @Test
    void documentReader_addsMetadataToAllDocuments() {
        val userId = UUID.randomUUID();
        val documentId = UUID.randomUUID();
        var entity = new com.marcos.leairning.documents.Document();
        entity.setId(documentId);
        entity.setUserId(userId);
        entity.setFileName("test.txt");
        entity.setContentType("text/plain");
        val ctx = DocumentContext.of("Hello world".getBytes(StandardCharsets.UTF_8), documentId)
                .withDocument(entity);
        val reader = pipeline.documentReader();
        var results = reader.apply(Flux.just(ctx)).collectList().block();
        assertNotNull(results);
        assertFalse(results.isEmpty());
        for (var doc : results) {
            assertEquals(userId.toString(), doc.getMetadata().get("userId"));
            assertEquals(documentId.toString(), doc.getMetadata().get("documentId"));
            assertEquals("test.txt", doc.getMetadata().get("documentTitle"));
            assertEquals("text/plain", doc.getMetadata().get("contentType"));
            assertEquals("txt", doc.getMetadata().get("documentType"));
        }
    }
}