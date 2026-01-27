package com.marcos.leairning.documents;

import com.marcos.leairning.jpa.AbstractJpaVersionedAuditableEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.UUID;

@Data
@Entity
@Table(name = "documents")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class Document extends AbstractJpaVersionedAuditableEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(name = "user")
    private UUID user;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "storage_path")
    private String storagePath;

}
