package com.marcos.documentsservice.pipeline;

import com.marcos.documentsservice.entity.Document;

/**
 * Context object for document processing pipeline.
 * Carries file bytes along with document metadata for processing.
 *
 * @param fileBytes Raw file content
 * @param documentId Database document ID
 * @param document Full document entity (null until enriched)
 */
public record DocumentContext(
        byte[] fileBytes,
        Long documentId,
        Document document
) {
    public static DocumentContext of(byte[] fileBytes, Long documentId) {
        return new DocumentContext(fileBytes, documentId, null);
    }

    public DocumentContext withDocument(Document document) {
        return new DocumentContext(fileBytes, documentId, document);
    }
}