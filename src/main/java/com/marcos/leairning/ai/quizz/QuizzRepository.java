package com.marcos.leairning.ai.quizz;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizzRepository extends JpaRepository<QuizzEntity, UUID> {

    Boolean existsByUserIdAndDocumentId(UUID userId, UUID documentId);

    Page<QuizzEntity> findAllByUserId(UUID userId, Pageable pageable);

    Optional<QuizzEntity> findByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
