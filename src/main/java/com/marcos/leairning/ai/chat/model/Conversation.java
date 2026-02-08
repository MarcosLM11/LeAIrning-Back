package com.marcos.leairning.ai.chat.model;

import com.marcos.leairning.documents.Document;
import com.marcos.leairning.util.jpa.AbstractJpaAuditableEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "conversations")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Conversation extends AbstractJpaAuditableEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "conversation_documents",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id")
    )
    private Set<Document> documents = new HashSet<>();

    public Set<UUID> getDocumentIds() {
        return documents.stream()
                .map(Document::getId)
                .collect(java.util.stream.Collectors.toSet());
    }
}
