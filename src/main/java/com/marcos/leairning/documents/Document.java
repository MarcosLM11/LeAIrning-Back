package com.marcos.leairning.documents;

import com.marcos.leairning.util.jpa.AbstractJpaAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@Table(name = "documents")
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Document extends AbstractJpaAuditableEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    private UUID id;
    private UUID userId;
    private String fileName;
    private String contentType;
    private Long size;
    private String storagePath;
    private String thumbnailPath;
    private DocumentStatus status;
}
