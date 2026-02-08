package com.marcos.leairning.ai.chat.util;

import com.marcos.leairning.ai.chat.dto.ConversationResponseDTO;
import com.marcos.leairning.ai.chat.model.Conversation;
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
