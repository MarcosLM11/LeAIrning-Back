package com.marcos.leairning.users;

import com.marcos.leairning.jpa.AbstractJpaVersionedAuditableEntity;
import jakarta.persistence.*;
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
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "role")
    private String role;

    @Column(name = "password")
    private String password;

    @Column(name = "verified", nullable = false)
    private boolean verified;
}
