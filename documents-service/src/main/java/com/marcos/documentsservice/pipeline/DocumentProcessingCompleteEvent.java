package com.marcos.documentsservice.pipeline;

/**
 * Event emitted when document processing completes successfully.
 *
 * @param documentId      ID of the processed document
 * @param vectorStoreId   ID of the document in the vector store
 * @param filePath        Path to the processed file
 * @param chunksProcessed Number of chunks that were processed
 */
public record DocumentProcessingCompleteEvent(
        Long documentId,
        String vectorStoreId,
        String filePath,
        int chunksProcessed
) {
}