package com.marcos.leairning.documents;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DocumentsRepository extends JpaRepository<Document, UUID> {
}
