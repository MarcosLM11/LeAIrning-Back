package com.marcos.leairning.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentsRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserId(UUID userId);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);
}