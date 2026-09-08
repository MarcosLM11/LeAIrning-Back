package com.marcos.leairning.users;

import com.marcos.leairning.util.jpa.AbstractJpaVersionedAuditableEntity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
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

    @Column(name = "provider", nullable = false)
    private String provider = "local";
}
