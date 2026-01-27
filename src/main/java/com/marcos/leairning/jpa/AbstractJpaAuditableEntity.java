package com.marcos.leairning.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class AbstractJpaAuditableEntity {

    @JsonIgnore
    @CreatedDate
    @Column(name = "created_timestamp", updatable = false)
    private Instant createdTimestamp;

    @JsonIgnore
    @LastModifiedDate
    @Column(name = "last_updated_timestamp")
    private Instant lastUpdatedTimestamp;
}
