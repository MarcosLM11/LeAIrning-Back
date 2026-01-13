package com.marcos.documentsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_user_id", columnList = "user_id"),
        @Index(name = "idx_documents_status", columnList = "status"),
        @Index(name = "idx_documents_created_at", columnList = "created_at")
})
public class Document extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, name = "original_filename")
    private String originalFilename;

    @Column(nullable = false, unique = true, name = "stored_filename")
    private String storedFilename;

    @Column(nullable = false, name = "content_type")
    private String contentType;

    @Column(nullable = false, name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "document_type")
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "vector_store_id")
    private String vectorStoreId;

    @Column(name = "extracted_text_hash")
    private String extractedTextHash;

    @Column(length = 1000, name = "error_message")
    private String errorMessage;
}