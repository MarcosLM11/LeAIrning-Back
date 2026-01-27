package com.marcos.leairning.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
public class AbstractJpaVersionedAuditableEntity extends AbstractJpaAuditableEntity {

    @Version
    @Column(name = "version")
    private Long version;
}
