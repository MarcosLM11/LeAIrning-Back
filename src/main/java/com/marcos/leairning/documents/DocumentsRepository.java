package com.marcos.leairning.documents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentsRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByUserId(UUID userId, Pageable pageable);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    Optional<Document> findByFileName(String fileName);

    List<Document> findByIdInAndUserId(List<UUID> ids, UUID userId);

    @Modifying
    @Query("DELETE FROM Document d WHERE d.id IN :ids AND d.userId = :userId")
    int deleteByIdInAndUserId(List<UUID> ids, UUID userId);

}
