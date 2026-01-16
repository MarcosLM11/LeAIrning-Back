package com.marcos.documentsservice.repository;

import com.marcos.documentsservice.entity.Document;
import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByUserId(Long userId, Pageable pageable);

    Page<Document> findByUserIdAndStatus(Long userId, ProcessingStatus status, Pageable pageable);

    List<Document> findByStatus(ProcessingStatus status);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, ProcessingStatus status);

    void deleteByUserId(Long userId);

    Optional<Document> findByStoredFilename(String storedFilename);

    Page<Document> findByUserIdAndDocumentType(Long userId, DocumentType type, Pageable pageable);
}