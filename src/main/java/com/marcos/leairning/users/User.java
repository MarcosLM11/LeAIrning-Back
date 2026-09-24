package com.marcos.leairning.users;

import com.marcos.leairning.jpa.JpaAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Entity @Builder
public class User extends JpaAuditableEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;
    @Column(name = "username", nullable = false)
    private String username;
    @Column(name = "password", nullable = false)
    private String password;
}