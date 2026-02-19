package com.marcos.leairning.ai.quizz;

import com.marcos.leairning.util.jpa.AbstractJpaVersionedAuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.UUID;

@Data
@Entity
@Table(name = "quizzs")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class QuizzEntity extends AbstractJpaVersionedAuditableEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    private UUID userId;
    private UUID documentId;
    private String quizz;
    private int lastScore;
}
