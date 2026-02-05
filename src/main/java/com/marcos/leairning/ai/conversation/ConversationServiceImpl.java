package com.marcos.leairning.ai.conversation;

import com.marcos.leairning.documents.Document;
import com.marcos.leairning.documents.DocumentsRepository;
import com.marcos.leairning.exception.ConversationNotFoundException;
import com.marcos.leairning.exception.DocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Flogger
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final DocumentsRepository documentsRepository;
    private final ConversationMapper mapper;

    @Override
    @Transactional
    public ConversationResponseDTO create(UUID userId, String title, Set<UUID> documentIds) {
        log.atInfo().log("Creating conversation for userId=%s with %d documents", userId, documentIds.size());

        // Validate that all documents belong to the user
        val documents = documentsRepository.findByIdInAndUserId(
                List.copyOf(documentIds),
                userId
        );

        if (documents.size() != documentIds.size()) {
            Set<UUID> foundIds = documents.stream()
                    .map(Document::getId)
                    .collect(java.util.stream.Collectors.toSet());
            Set<UUID> missingIds = documentIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(java.util.stream.Collectors.toSet());
            throw new DocumentNotFoundException("Documents not found or not accessible: " + missingIds);
        }

        val conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setDocuments(new HashSet<>(documents));

        val saved = conversationRepository.save(conversation);
        log.atInfo().log("Created conversation id=%s", saved.getId());

        return mapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponseDTO> findAllByUser(UUID userId, Pageable pageable) {
        return conversationRepository
                .findByUserIdOrderByLastUpdatedTimestampDesc(userId, pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponseDTO findById(UUID userId, UUID conversationId) {
        return conversationRepository
                .findByIdAndUserIdWithDocuments(conversationId, userId)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ConversationNotFoundException(
                        "Conversation not found: " + conversationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getDocumentIds(UUID userId, UUID conversationId) {
        val conversation = conversationRepository
                .findByIdAndUserIdWithDocuments(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(
                        "Conversation not found: " + conversationId));

        return conversation.getDocumentIds();
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) {
            throw new ConversationNotFoundException("Conversation not found: " + conversationId);
        }
        conversationRepository.deleteByIdAndUserId(conversationId, userId);
        log.atInfo().log("Deleted conversation id=%s for userId=%s", conversationId, userId);
    }

    @Override
    @Transactional
    public ConversationResponseDTO updateTitle(UUID userId, UUID conversationId, String newTitle) {
        val conversation = conversationRepository
                .findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(
                        "Conversation not found: " + conversationId));

        conversation.setTitle(newTitle);
        val saved = conversationRepository.save(conversation);

        return mapper.toDTO(saved);
    }
}
