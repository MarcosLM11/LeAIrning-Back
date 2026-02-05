package com.marcos.leairning.ai.conversation;

import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationResponseDTO toDTO(Conversation entity) {
        return new ConversationResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getDocumentIds(),
                entity.getCreatedTimestamp(),
                entity.getLastUpdatedTimestamp()
        );
    }
}
