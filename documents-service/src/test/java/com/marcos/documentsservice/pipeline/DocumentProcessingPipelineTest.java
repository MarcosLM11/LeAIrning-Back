package com.marcos.documentsservice.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for DocumentProcessingPipeline functions.
 * These tests define the expected behavior before implementation (Red phase).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentProcessingPipeline Unit Tests")
class DocumentProcessingPipelineTest {

    @Mock
    private VectorStore vectorStore;

    private DocumentProcessingPipeline pipeline;

    @BeforeEach
    void setUp() {
        // Create pipeline instance
        pipeline = new DocumentProcessingPipeline();
    }

    /**
     * Creates a simple character-based TextSplitter for testing.
     * This splitter doesn't require external services like Ollama.
     *
     * @param chunkSize Size of each chunk in characters
     * @return Simple TextSplitter implementation
     */
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
            public List<Document> apply(List<Document> documents) {
                List<Document> allChunks = new ArrayList<>();

                for (Document doc : documents) {
                    String text = doc.getText();

                    if (text == null || text.isEmpty()) {
                        allChunks.add(doc);
                        continue;
                    }

                    // Split text into chunks of specified size
                    for (int i = 0; i < text.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, text.length());
                        String chunkText = text.substring(i, end);

                        Document chunk = new Document(chunkText);
                        // Copy metadata from original document
                        chunk.getMetadata().putAll(doc.getMetadata());
                        allChunks.add(chunk);
                    }
                }

                return allChunks;
            }
        };
    }

    // ==================== documentReader Tests ====================

    @Test
    @DisplayName("documentReader: Should convert byte array to Document successfully")
    void documentReaderShouldConvertByteArrayToDocument() throws IOException {
        // Given - Load a test PDF file
        Path testPdfPath = Path.of("src/test/resources/test-files/test-document.pdf");
        byte[] pdfBytes = Files.readAllBytes(testPdfPath);

        Function<Flux<byte[]>, Flux<Document>> documentReader = pipeline.documentReader();

        // When
        Flux<Document> result = documentReader.apply(Flux.just(pdfBytes));

        // Then - Verify document is extracted
        StepVerifier.create(result)
                .assertNext(document -> {
                    assertNotNull(document);
                    assertNotNull(document.getText());
                    assertFalse(document.getText().isEmpty());
                    assertTrue(document.getText().length() > 0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("documentReader: Should handle multiple files in stream")
    void documentReaderShouldHandleMultipleFilesInStream() throws IOException {
        // Given - Two test files
        byte[] pdfBytes1 = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document.pdf"));
        byte[] pdfBytes2 = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document2.pdf"));

        Function<Flux<byte[]>, Flux<Document>> documentReader = pipeline.documentReader();

        // When
        Flux<Document> result = documentReader.apply(Flux.just(pdfBytes1, pdfBytes2));

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("documentReader: Should handle invalid file gracefully")
    void documentReaderShouldHandleInvalidFileGracefully() {
        // Given - Invalid bytes (TikaDocumentReader returns empty document for invalid files)
        byte[] invalidBytes = new byte[]{0x00, 0x01, 0x02};

        Function<Flux<byte[]>, Flux<Document>> documentReader = pipeline.documentReader();

        // When
        Flux<Document> result = documentReader.apply(Flux.just(invalidBytes));

        // Then - Tika returns a document with empty text instead of throwing exception
        StepVerifier.create(result)
                .assertNext(document -> {
                    assertNotNull(document);
                    assertNotNull(document.getText());
                    assertTrue(document.getText().isEmpty(), "Invalid file should produce empty document");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("documentReader: Should extract text from TXT file")
    void documentReaderShouldExtractTextFromTxtFile() throws IOException {
        // Given
        byte[] txtBytes = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document.txt"));

        Function<Flux<byte[]>, Flux<Document>> documentReader = pipeline.documentReader();

        // When
        Flux<Document> result = documentReader.apply(Flux.just(txtBytes));

        // Then
        StepVerifier.create(result)
                .assertNext(document -> {
                    assertNotNull(document.getText());
                    assertTrue(document.getText().contains("test"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("documentReader: Should extract text from DOCX file")
    void documentReaderShouldExtractTextFromDocxFile() throws IOException {
        // Given
        byte[] docxBytes = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document.docx"));

        Function<Flux<byte[]>, Flux<Document>> documentReader = pipeline.documentReader();

        // When
        Flux<Document> result = documentReader.apply(Flux.just(docxBytes));

        // Then
        StepVerifier.create(result)
                .assertNext(document -> {
                    assertNotNull(document.getText());
                    assertFalse(document.getText().isEmpty());
                })
                .verifyComplete();
    }

    // ==================== splitter Tests ====================

    @Test
    @DisplayName("splitter: Should split large document into chunks")
    void splitterShouldSplitLargeDocumentIntoChunks() {
        // Given - Document with 5000 characters (should create multiple chunks)
        String longText = "A".repeat(5000);
        Document largeDocument = new Document(longText);

        // Use simple splitter without Ollama dependency
        TextSplitter simpleSplitter = createSimpleTextSplitter(500);
        Function<Flux<Document>, Flux<List<Document>>> splitter = pipeline.splitter(simpleSplitter);

        // When
        Flux<List<Document>> result = splitter.apply(Flux.just(largeDocument));

        // Then
        StepVerifier.create(result)
                .assertNext(chunks -> {
                    assertNotNull(chunks);
                    assertTrue(chunks.size() > 1, "Should split into multiple chunks");
                    assertTrue(chunks.size() >= 10, "5000 chars with chunk size 500 should create ~10 chunks");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("splitter: Should handle small document without splitting")
    void splitterShouldHandleSmallDocumentWithoutSplitting() {
        // Given - Small document (< 500 chars)
        String shortText = "This is a short document with less than 500 characters.";
        Document smallDocument = new Document(shortText);

        // Use simple splitter without Ollama dependency
        TextSplitter simpleSplitter = createSimpleTextSplitter(500);
        Function<Flux<Document>, Flux<List<Document>>> splitter = pipeline.splitter(simpleSplitter);

        // When
        Flux<List<Document>> result = splitter.apply(Flux.just(smallDocument));

        // Then
        StepVerifier.create(result)
                .assertNext(chunks -> {
                    assertNotNull(chunks);
                    assertEquals(1, chunks.size(), "Small document should result in 1 chunk");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("splitter: Should preserve text content in chunks")
    void splitterShouldPreserveTextContentInChunks() {
        // Given
        String originalText = "Test content. ".repeat(100); // ~1400 characters
        Document document = new Document(originalText);

        // Use simple splitter without Ollama dependency
        TextSplitter simpleSplitter = createSimpleTextSplitter(500);
        Function<Flux<Document>, Flux<List<Document>>> splitter = pipeline.splitter(simpleSplitter);

        // When
        Flux<List<Document>> result = splitter.apply(Flux.just(document));

        // Then
        StepVerifier.create(result)
                .assertNext(chunks -> {
                    assertNotNull(chunks);
                    assertTrue(chunks.size() > 1);

                    // Verify all chunks contain parts of original text
                    chunks.forEach(chunk -> {
                        assertNotNull(chunk.getText());
                        assertFalse(chunk.getText().isEmpty());
                        assertTrue(chunk.getText().contains("Test content"));
                    });
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("splitter: Should handle empty document")
    void splitterShouldHandleEmptyDocument() {
        // Given
        Document emptyDocument = new Document("");

        // Use simple splitter without Ollama dependency
        TextSplitter simpleSplitter = createSimpleTextSplitter(500);
        Function<Flux<Document>, Flux<List<Document>>> splitter = pipeline.splitter(simpleSplitter);

        // When
        Flux<List<Document>> result = splitter.apply(Flux.just(emptyDocument));

        // Then
        StepVerifier.create(result)
                .assertNext(chunks -> {
                    assertNotNull(chunks);
                    // May return empty list or single empty chunk depending on implementation
                    assertTrue(chunks.isEmpty() || (chunks.size() == 1 && chunks.getFirst().getText().isEmpty()));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("splitter: Should handle multiple documents in stream")
    void splitterShouldHandleMultipleDocumentsInStream() {
        // Given
        Document doc1 = new Document("A".repeat(1000));
        Document doc2 = new Document("B".repeat(1000));

        // Use simple splitter without Ollama dependency
        TextSplitter simpleSplitter = createSimpleTextSplitter(500);
        Function<Flux<Document>, Flux<List<Document>>> splitter = pipeline.splitter(simpleSplitter);

        // When
        Flux<List<Document>> result = splitter.apply(Flux.just(doc1, doc2));

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    // ==================== vectorStoreConsumer Tests ====================

    @Test
    @DisplayName("vectorStoreConsumer: Should write chunks to vector store")
    void vectorStoreConsumerShouldWriteChunksToVectorStore() {
        // Given
        Document chunk1 = new Document("Chunk 1 content");
        Document chunk2 = new Document("Chunk 2 content");

        List<Document> chunks = List.of(chunk1, chunk2);

        doNothing().when(vectorStore).accept(anyList());

        Consumer<Flux<List<Document>>> vectorStoreConsumer =
                pipeline.vectorStoreConsumer(vectorStore);

        // When
        vectorStoreConsumer.accept(Flux.just(chunks));

        // Then
        // Give time for async processing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(vectorStore, times(1)).accept(argThat(docs -> docs.size() == 2));
    }

    @Test
    @DisplayName("vectorStoreConsumer: Should skip empty chunk lists")
    void vectorStoreConsumerShouldSkipEmptyChunkLists() {
        // Given
        List<Document> emptyChunks = List.of();

        Consumer<Flux<List<Document>>> vectorStoreConsumer =
                pipeline.vectorStoreConsumer(vectorStore);

        // When
        vectorStoreConsumer.accept(Flux.just(emptyChunks));

        // Then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(vectorStore, never()).accept(anyList());
    }

    @Test
    @DisplayName("vectorStoreConsumer: Should handle multiple batches")
    void vectorStoreConsumerShouldHandleMultipleBatches() {
        // Given
        List<Document> batch1 = List.of(new Document("Batch 1"));
        List<Document> batch2 = List.of(new Document("Batch 2"));

        doNothing().when(vectorStore).accept(anyList());

        Consumer<Flux<List<Document>>> vectorStoreConsumer =
                pipeline.vectorStoreConsumer(vectorStore);

        // When
        vectorStoreConsumer.accept(Flux.just(batch1, batch2));

        // Then
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(vectorStore, times(2)).accept(anyList());
    }

    @Test
    @DisplayName("vectorStoreConsumer: Should handle vector store errors")
    void vectorStoreConsumerShouldHandleVectorStoreErrors() {
        // Given
        Document chunk = new Document("Test content");
        List<Document> chunks = List.of(chunk);

        doThrow(new RuntimeException("Vector store connection error"))
                .when(vectorStore).accept(anyList());

        Consumer<Flux<List<Document>>> vectorStoreConsumer =
                pipeline.vectorStoreConsumer(vectorStore);

        // When & Then - Should not crash the application
        assertDoesNotThrow(() -> {
            vectorStoreConsumer.accept(Flux.just(chunks));
            Thread.sleep(1000);
        });
    }

    @Test
    @DisplayName("vectorStoreConsumer: Should preserve metadata in stored documents")
    void vectorStoreConsumerShouldPreserveMetadataInStoredDocuments() {
        // Given
        Document chunk = new Document("Test content");
        chunk.getMetadata().put("documentTitle", "test_title");
        chunk.getMetadata().put("userId", 123L);
        chunk.getMetadata().put("documentId", 456L);

        List<Document> chunks = List.of(chunk);

        doNothing().when(vectorStore).accept(anyList());

        Consumer<Flux<List<Document>>> vectorStoreConsumer =
                pipeline.vectorStoreConsumer(vectorStore);

        // When
        vectorStoreConsumer.accept(Flux.just(chunks));

        // Then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(vectorStore).accept(argThat(docs -> {
            Document storedDoc = docs.get(0);
            return storedDoc.getMetadata().containsKey("documentTitle") &&
                   storedDoc.getMetadata().containsKey("userId") &&
                   storedDoc.getMetadata().containsKey("documentId");
        }));
    }
}