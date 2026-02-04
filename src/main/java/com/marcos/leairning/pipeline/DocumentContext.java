package com.marcos.leairning.pipeline;

import com.marcos.leairning.documents.Document;
import java.util.UUID;

/**
 * Context object for a document processing pipeline.
 * Carries file bytes along with document metadata for processing.
 *
 * @param fileBytes Raw file content
 * @param documentId Database document ID
 * @param document Full document entity (null until enriched)
 */
public record DocumentContext(
        byte[] fileBytes,
        UUID documentId,
        Document document
) {
    public static DocumentContext of(byte[] fileBytes, UUID documentId) {
        return new DocumentContext(fileBytes, documentId, null);
    }

    public DocumentContext withDocument(Document document) {
        return new DocumentContext(fileBytes, documentId, document);
    }
}
