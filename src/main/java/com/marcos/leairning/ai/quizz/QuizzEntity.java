package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.util.jpa.AbstractJpaVersionedAuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quizzs")
public class QuizzEntity extends AbstractJpaVersionedAuditableEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    private UUID userId;
    private UUID documentId;
    private String quizz;
    private int lastScore;
}
