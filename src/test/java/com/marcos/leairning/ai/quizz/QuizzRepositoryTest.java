package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.AbstractRepositoryTest;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class QuizzRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    QuizzRepository repository;

    UUID userId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        userId = UUID.randomUUID();
    }

    @Test
    void existsByUserIdAndDocumentId_existingQuizz_returnsTrue() {
        val docId = UUID.randomUUID();
        repository.save(createQuizz(userId, docId));
        assertTrue(repository.existsByUserIdAndDocumentId(userId, docId));
    }

    @Test
    void existsByUserIdAndDocumentId_nonExisting_returnsFalse() {
        assertFalse(repository.existsByUserIdAndDocumentId(userId, UUID.randomUUID()));
    }

    @Test
    void findAllByUserId_returnsOnlyUserQuizzes() {
        val otherId = UUID.randomUUID();
        repository.save(createQuizz(userId, UUID.randomUUID()));
        repository.save(createQuizz(userId, UUID.randomUUID()));
        repository.save(createQuizz(otherId, UUID.randomUUID()));
        val page = repository.findAllByUserId(userId, PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findByIdAndUserId_matchingBoth_returnsQuizz() {
        val quizz = createQuizz(userId, UUID.randomUUID());
        repository.save(quizz);
        val result = repository.findByIdAndUserId(quizz.getId(), userId);
        assertTrue(result.isPresent());
    }

    @Test
    void findByIdAndUserId_wrongUser_returnsEmpty() {
        val quizz = createQuizz(userId, UUID.randomUUID());
        repository.save(quizz);
        val result = repository.findByIdAndUserId(quizz.getId(), UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteByIdAndUserId_removesQuizz() {
        val quizz = createQuizz(userId, UUID.randomUUID());
        repository.save(quizz);
        repository.deleteByIdAndUserId(quizz.getId(), userId);
        repository.flush();
        assertEquals(0, repository.count());
    }

    private QuizzEntity createQuizz(UUID ownerUserId, UUID documentId) {
        val quizz = new QuizzEntity();
        quizz.setUserId(ownerUserId);
        quizz.setDocumentId(documentId);
        quizz.setQuizz("{\"questions\":[]}");
        quizz.setLastScore(0);
        return quizz;
    }
}
