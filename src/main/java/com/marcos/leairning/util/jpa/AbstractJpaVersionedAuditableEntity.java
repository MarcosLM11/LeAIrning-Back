package com.marcos.leairning.util.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@MappedSuperclass
@AllArgsConstructor
@NoArgsConstructor
public class AbstractJpaVersionedAuditableEntity extends AbstractJpaAuditableEntity {

    @Version
    @Column(name = "version")
    private Long version;
}
