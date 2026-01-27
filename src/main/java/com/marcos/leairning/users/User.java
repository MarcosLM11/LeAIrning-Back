package com.marcos.leairning.users;

import com.marcos.leairning.jpa.AbstractJpaVersionedAuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class User extends AbstractJpaVersionedAuditableEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    private String email;
    private String password;
}
