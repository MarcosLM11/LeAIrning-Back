package com.marcos.documentsservice.pipeline;

import com.marcos.documentsservice.entity.Document;
import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import com.marcos.documentsservice.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentProcessingPipeline Unit Tests")
class DocumentProcessingPipelineTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentRepository documentRepository;

    private DocumentProcessingPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new DocumentProcessingPipeline(documentRepository);
    }

    private Document createTestDocumentEntity(Long id, Long userId, String filename) {
        var doc = new Document();
        doc.setId(id);
        doc.setUserId(userId);
        doc.setOriginalFilename(filename);
        doc.setStoredFilename("stored_" + filename);
        doc.setContentType("application/pdf");
        doc.setFileSize(1000L);
        doc.setDocumentType(DocumentType.PDF);
        doc.setStatus(ProcessingStatus.UPLOADED);
        return doc;
    }

    private TextSplitter createSimpleTextSplitter(int chunkSize) {
        return new TextSplitter() {
            @Override
            protected List<String> splitText(String text) {
                List<String> chunks = new ArrayList<>();
                if (text == null || text.isEmpty()) {
                    return chunks;
                }
                for (int i = 0; i < text.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, text.length());
                    chunks.add(text.substring(i, end));
                }
                return chunks;
            }

            @Override
            public List<org.springframework.ai.document.Document> apply(List<org.springframework.ai.document.Document> documents) {
                List<org.springframework.ai.document.Document> allChunks = new ArrayList<>();
                for (org.springframework.ai.document.Document doc : documents) {
                    String text = doc.getText();
                    if (text == null || text.isEmpty()) {
                        allChunks.add(doc);
                        continue;
                    }
                    for (int i = 0; i < text.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, text.length());
                        String chunkText = text.substring(i, end);
                        org.springframework.ai.document.Document chunk = new org.springframework.ai.document.Document(chunkText);
                        chunk.getMetadata().putAll(doc.getMetadata());
                        allChunks.add(chunk);
                    }
                }
                return allChunks;
            }
        };
    }

    // ==================== metadataEnricher Tests ====================

    @Test
    @DisplayName("metadataEnricher: Should enrich context with document entity")
    void metadataEnricherShouldEnrichContextWithDocumentEntity() {
        var documentEntity = createTestDocumentEntity(123L, 456L, "test.pdf");
        when(documentRepository.findById(123L)).thenReturn(Optional.of(documentEntity));
        var context = DocumentContext.of(new byte[]{1, 2, 3}, 123L);
        Function<Flux<DocumentContext>, Flux<DocumentContext>> metadataEnricher = pipeline.metadataEnricher();

        StepVerifier.create(metadataEnricher.apply(Flux.just(context)))
                .assertNext(enrichedContext -> {
                    assertNotNull(enrichedContext.document());
                    assertEquals(123L, enrichedContext.document().getId());
                    assertEquals(456L, enrichedContext.document().getUserId());
                    assertEquals("test.pdf", enrichedContext.document().getOriginalFilename());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("metadataEnricher: Should throw exception when document not found")
    void metadataEnricherShouldThrowExceptionWhenDocumentNotFound() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());
        var context = DocumentContext.of(new byte[]{1, 2, 3}, 999L);
        Function<Flux<DocumentContext>, Flux<DocumentContext>> metadataEnricher = pipeline.metadataEnricher();

        StepVerifier.create(metadataEnricher.apply(Flux.just(context)))
                .expectError()
                .verify();
    }

    // ==================== documentReader Tests ====================

    @Test
    @DisplayName("documentReader: Should convert DocumentContext to Document with metadata")
    void documentReaderShouldConvertContextToDocumentWithMetadata() throws IOException {
        var documentEntity = createTestDocumentEntity(123L, 456L, "test-document.pdf");
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document.pdf"));
        var context = new DocumentContext(pdfBytes, 123L, documentEntity);
        Function<Flux<DocumentContext>, Flux<org.springframework.ai.document.Document>> documentReader = pipeline.documentReader();

        StepVerifier.create(documentReader.apply(Flux.just(context)))
                .assertNext(document -> {
                    assertNotNull(document);
                    assertNotNull(document.getText());
                    assertFalse(document.getText().isEmpty());
                    assertEquals(456L, document.getMetadata().get("userId"));
                    assertEquals(123L, document.getMetadata().get("documentId"));
                    assertEquals("test-document.pdf", document.getMetadata().get("documentTitle"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("documentReader: Should handle multiple files in stream")
    void documentReaderShouldHandleMultipleFilesInStream() throws IOException {
        var doc1 = createTestDocumentEntity(1L, 100L, "doc1.pdf");
        var doc2 = createTestDocumentEntity(2L, 200L, "doc2.pdf");
        byte[] pdfBytes1 = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document.pdf"));
        byte[] pdfBytes2 = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document2.pdf"));
        var context1 = new DocumentContext(pdfBytes1, 1L, doc1);
        var context2 = new DocumentContext(pdfBytes2, 2L, doc2);
        Function<Flux<DocumentContext>, Flux<org.springframework.ai.document.Document>> documentReader = pipeline.documentReader();

        StepVerifier.create(documentReader.apply(Flux.just(context1, context2)))
                .assertNext(document -> assertEquals(100L, document.getMetadata().get("userId")))
                .assertNext(document -> assertEquals(200L, document.getMetadata().get("userId")))
                .verifyComplete();
    }

    @Test
    @DisplayName("documentReader: Should extract text from TXT file with metadata")
    void documentReaderShouldExtractTextFromTxtFileWithMetadata() throws IOException {
        var documentEntity = createTestDocumentEntity(123L, 456L, "test.txt");
        documentEntity.setContentType("text/plain");
        documentEntity.setDocumentType(DocumentType.TXT);
        byte[] txtBytes = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document.txt"));
        var context = new DocumentContext(txtBytes, 123L, documentEntity);
        Function<Flux<DocumentContext>, Flux<org.springframework.ai.document.Document>> documentReader = pipeline.documentReader();

        StepVerifier.create(documentReader.apply(Flux.just(context)))
                .assertNext(document -> {
                    assertNotNull(document.getText());
                    assertTrue(document.getText().contains("test"));
                    assertEquals(456L, document.getMetadata().get("userId"));
                })
                .verifyComplete();
    }

    // ==================== splitter Tests ====================

    @Test
    @DisplayName("splitter: Should split large document into chunks preserving metadata")
    void splitterShouldSplitLargeDocumentIntoChunksPreservingMetadata() {
        String longText = "A".repeat(5000);
        org.springframework.ai.document.Document largeDocument = new org.springframework.ai.document.Document(longText);
        largeDocument.getMetadata().put("userId", 123L);
        largeDocument.getMetadata().put("documentId", 456L);
        TextSplitter simpleSplitter = createSimpleTextSplitter(500);
        Function<Flux<org.springframework.ai.document.Document>, Flux<List<org.springframework.ai.document.Document>>> splitter = pipeline.splitter(simpleSplitter);

        StepVerifier.create(splitter.apply(Flux.just(largeDocument)))
                .assertNext(chunks -> {
                    assertNotNull(chunks);
                    assertTrue(chunks.size() >= 10);
                    chunks.forEach(chunk -> {
                        assertEquals(123L, chunk.getMetadata().get("userId"));
                        assertEquals(456L, chunk.getMetadata().get("documentId"));
                    });
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("splitter: Should handle small document without splitting")
    void splitterShouldHandleSmallDocumentWithoutSplitting() {
        String shortText = "This is a short document with less than 500 characters.";
        org.springframework.ai.document.Document smallDocument = new org.springframework.ai.document.Document(shortText);
        TextSplitter simpleSplitter = createSimpleTextSplitter(500);
        Function<Flux<org.springframework.ai.document.Document>, Flux<List<org.springframework.ai.document.Document>>> splitter = pipeline.splitter(simpleSplitter);

        StepVerifier.create(splitter.apply(Flux.just(smallDocument)))
                .assertNext(chunks -> {
                    assertNotNull(chunks);
                    assertEquals(1, chunks.size());
                })
                .verifyComplete();
    }

    // ==================== vectorStoreConsumer Tests ====================

    @Test
    @DisplayName("vectorStoreConsumer: Should write chunks to vector store")
    void vectorStoreConsumerShouldWriteChunksToVectorStore() throws InterruptedException {
        org.springframework.ai.document.Document chunk1 = new org.springframework.ai.document.Document("Chunk 1 content");
        org.springframework.ai.document.Document chunk2 = new org.springframework.ai.document.Document("Chunk 2 content");
        List<org.springframework.ai.document.Document> chunks = List.of(chunk1, chunk2);
        doNothing().when(vectorStore).accept(anyList());
        Consumer<Flux<List<org.springframework.ai.document.Document>>> vectorStoreConsumer = pipeline.vectorStoreConsumer(vectorStore);

        vectorStoreConsumer.accept(Flux.just(chunks));
        Thread.sleep(1000);

        verify(vectorStore, times(1)).accept(argThat(docs -> docs.size() == 2));
    }

    @Test
    @DisplayName("vectorStoreConsumer: Should skip empty chunk lists")
    void vectorStoreConsumerShouldSkipEmptyChunkLists() throws InterruptedException {
        List<org.springframework.ai.document.Document> emptyChunks = List.of();
        Consumer<Flux<List<org.springframework.ai.document.Document>>> vectorStoreConsumer = pipeline.vectorStoreConsumer(vectorStore);

        vectorStoreConsumer.accept(Flux.just(emptyChunks));
        Thread.sleep(500);

        verify(vectorStore, never()).accept(anyList());
    }

    @Test
    @DisplayName("vectorStoreConsumer: Should preserve metadata in stored documents")
    void vectorStoreConsumerShouldPreserveMetadataInStoredDocuments() throws InterruptedException {
        org.springframework.ai.document.Document chunk = new org.springframework.ai.document.Document("Test content");
        chunk.getMetadata().put("documentTitle", "test_title");
        chunk.getMetadata().put("userId", 123L);
        chunk.getMetadata().put("documentId", 456L);
        List<org.springframework.ai.document.Document> chunks = List.of(chunk);
        doNothing().when(vectorStore).accept(anyList());
        Consumer<Flux<List<org.springframework.ai.document.Document>>> vectorStoreConsumer = pipeline.vectorStoreConsumer(vectorStore);

        vectorStoreConsumer.accept(Flux.just(chunks));
        Thread.sleep(500);

        verify(vectorStore).accept(argThat(docs -> {
            org.springframework.ai.document.Document storedDoc = docs.get(0);
            return storedDoc.getMetadata().containsKey("documentTitle") &&
                   storedDoc.getMetadata().containsKey("userId") &&
                   storedDoc.getMetadata().containsKey("documentId");
        }));
    }
}