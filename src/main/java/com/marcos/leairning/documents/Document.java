package com.marcos.leairning.documents;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@Table(name = "documents")
@AllArgsConstructor
@NoArgsConstructor
public class Document extends JpaAuditableEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", columnDefinition = "bytea")
    private byte[] content;
}