package com.marcos.leairning.ai.chat.repository;

import com.marcos.leairning.ai.chat.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findByUserIdOrderByLastUpdatedTimestampDesc(UUID userId, Pageable pageable);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT c FROM Conversation c LEFT JOIN FETCH c.documents WHERE c.id = :id AND c.userId = :userId")
    Optional<Conversation> findByIdAndUserIdWithDocuments(@Param("id") UUID id, @Param("userId") UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
