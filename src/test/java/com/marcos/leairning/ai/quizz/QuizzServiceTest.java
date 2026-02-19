package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.documents.DocumentsService;
import com.marcos.leairning.exception.QuizzNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizzServiceTest {

    @Mock
    QuizzRepository quizzRepository;
    @Mock
    DocumentsService documentsService;
    @Mock
    ChatClient.Builder chatClientBuilder;
    @Mock
    ObjectMapper objectMapper;
    @InjectMocks
    QuizzService quizzService;

    @Test
    void generateQuizz_returnsGeneratedQuizzWithId() {
        var userId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        var entityId = UUID.randomUUID();
        var questions = List.of(new Question("What?", "This", QuestionType.MEDIUM));
        var quizz = new Quizz(questions);
        when(documentsService.downloadDocument(userId, documentId)).thenReturn("content".getBytes());
        var chatClient = mock(ChatClient.class);
        var builder2 = mock(ChatClient.Builder.class);
        when(chatClientBuilder.clone()).thenReturn(builder2);
        when(builder2.build()).thenReturn(chatClient);
        var prompt = mock(ChatClient.ChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(prompt);
        when(prompt.system(any(java.util.function.Consumer.class))).thenReturn(prompt);
        when(prompt.user(anyString())).thenReturn(prompt);
        var callResponse = mock(ChatClient.CallResponseSpec.class);
        when(prompt.call()).thenReturn(callResponse);
        when(callResponse.entity(Quizz.class)).thenReturn(quizz);
        when(objectMapper.writeValueAsString(quizz)).thenReturn("{\"questions\":[]}");
        when(quizzRepository.save(any(QuizzEntity.class))).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, QuizzEntity.class);
            entity.setId(entityId);
            return entity;
        });
        var result = quizzService.generateQuizz(userId, documentId, 5, QuestionType.MEDIUM);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(entityId);
        assertThat(result.questions()).isEqualTo(questions);
    }

    @Test
    void getQuizz_withOwnership_returnsQuizz() {
        var userId = UUID.randomUUID();
        var quizzId = UUID.randomUUID();
        var entity = new QuizzEntity();
        entity.setId(quizzId);
        entity.setUserId(userId);
        entity.setQuizz("{\"questions\":[]}");
        var quizz = new Quizz(List.of());
        when(quizzRepository.findByIdAndUserId(quizzId, userId)).thenReturn(Optional.of(entity));
        when(objectMapper.readValue("{\"questions\":[]}", Quizz.class)).thenReturn(quizz);
        var result = quizzService.getQuizz(userId, quizzId);
        assertThat(result).isEqualTo(quizz);
    }

    @Test
    void getQuizz_withoutOwnership_throwsNotFound() {
        var userId = UUID.randomUUID();
        var quizzId = UUID.randomUUID();
        when(quizzRepository.findByIdAndUserId(quizzId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> quizzService.getQuizz(userId, quizzId))
                .isInstanceOf(QuizzNotFoundException.class);
    }

    @Test
    void deleteQuizz_withOwnership_deletes() {
        var userId = UUID.randomUUID();
        var quizzId = UUID.randomUUID();
        var entity = new QuizzEntity();
        entity.setId(quizzId);
        entity.setUserId(userId);
        when(quizzRepository.findByIdAndUserId(quizzId, userId)).thenReturn(Optional.of(entity));
        quizzService.deleteQuizz(userId, quizzId);
        verify(quizzRepository).deleteByIdAndUserId(quizzId, userId);
    }

    @Test
    void updateQuizzScore_withOwnership_updatesScore() {
        var userId = UUID.randomUUID();
        var quizzId = UUID.randomUUID();
        var entity = new QuizzEntity();
        entity.setId(quizzId);
        entity.setUserId(userId);
        entity.setLastScore(0);
        when(quizzRepository.findByIdAndUserId(quizzId, userId)).thenReturn(Optional.of(entity));
        quizzService.updateQuizzScore(userId, quizzId, 85);
        assertThat(entity.getLastScore()).isEqualTo(85);
        verify(quizzRepository).save(entity);
    }

    @Test
    void updateQuizzScore_withoutOwnership_throwsNotFound() {
        var userId = UUID.randomUUID();
        var quizzId = UUID.randomUUID();
        when(quizzRepository.findByIdAndUserId(quizzId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> quizzService.updateQuizzScore(userId, quizzId, 85))
                .isInstanceOf(QuizzNotFoundException.class);
    }
}
