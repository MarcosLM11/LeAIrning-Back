package com.marcos.documentsservice.pipeline;

/**
 * Event emitted when document processing fails.
 *
 * @param documentId   ID of the failed document
 * @param errorMessage Description of the error
 * @param filePath     Path to the failed file
 */
public record DocumentProcessingFailedEvent(
        Long documentId,
        String errorMessage,
        String filePath
) {
}