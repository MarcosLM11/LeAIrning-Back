package com.marcos.leairning.ai.chat.service;

import com.marcos.leairning.ai.chat.model.Conversation;
import com.marcos.leairning.ai.chat.util.ConversationMapper;
import com.marcos.leairning.ai.chat.repository.ConversationRepository;
import com.marcos.leairning.ai.chat.dto.ConversationResponseDTO;
import com.marcos.leairning.documents.Document;
import com.marcos.leairning.documents.DocumentsRepository;
import com.marcos.leairning.exception.ConversationNotFoundException;
import com.marcos.leairning.exception.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationServiceImpl.class);
    private final ConversationRepository conversationRepository;
    private final DocumentsRepository documentsRepository;
    private final ConversationMapper mapper;

    public ConversationServiceImpl(ConversationRepository conversationRepository, DocumentsRepository documentsRepository, ConversationMapper mapper) {
        this.conversationRepository = conversationRepository;
        this.documentsRepository = documentsRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ConversationResponseDTO create(UUID userId, String title, Set<UUID> documentIds) {
        log.info("Creating conversation for userId={} with {} documents", userId, documentIds.size());

        // Validate that all documents belong to the user
        var documents = documentsRepository.findByIdInAndUserId(
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

        var conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setDocuments(new HashSet<>(documents));

        var saved = conversationRepository.save(conversation);
        log.info("Created conversation id={}", saved.getId());
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
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getDocumentIds(UUID userId, UUID conversationId) {
        var conversation = conversationRepository
                .findByIdAndUserIdWithDocuments(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        return conversation.getDocumentIds();
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) {
            throw new ConversationNotFoundException("Conversation not found: " + conversationId);
        }
        conversationRepository.deleteByIdAndUserId(conversationId, userId);
        log.atInfo().log("Deleted conversation id={} for userId={}", conversationId, userId);
    }

    @Override
    @Transactional
    public ConversationResponseDTO updateTitle(UUID userId, UUID conversationId, String newTitle) {
        var conversation = conversationRepository
                .findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        conversation.setTitle(newTitle);
        var saved = conversationRepository.save(conversation);

        return mapper.toDTO(saved);
    }
}