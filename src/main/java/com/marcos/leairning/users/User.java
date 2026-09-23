package com.marcos.leairning.users;

import com.marcos.leairning.util.jpa.AbstractJpaAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Entity @Builder
public class User extends AbstractJpaAuditableEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "name")
    private String username;
    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "password")
    private String password;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "provider", nullable = false)
    private String provider = "local";
}
