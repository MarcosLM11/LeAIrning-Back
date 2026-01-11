package com.marcos.usersservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class AbstractAuditableEntity {

    @Version
    @Column
    private Long version;

    @JsonIgnore
    @CreatedDate
    @Column(name = "created_timestamp", updatable = false)
    private Instant createdTimestamp;

    @JsonIgnore
    @LastModifiedDate
    @Column(name = "last_updated_timestamp")
    private Instant lastUpdatedTimestamp;
}
