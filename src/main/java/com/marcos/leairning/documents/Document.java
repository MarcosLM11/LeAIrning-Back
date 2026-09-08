package com.marcos.leairning.documents;

import com.marcos.leairning.util.jpa.AbstractJpaVersionedAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import lombok.ToString;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "documents")
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Document extends AbstractJpaVersionedAuditableEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "storage_path")
    private String storagePath;

}
