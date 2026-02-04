package com.marcos.leairning.documents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface DocumentsService {

    Page<DocumentResponseDTO> getDocuments(UUID userId, Pageable pageable);

    List<DocumentResponseDTO> upload(UUID userId, List<MultipartFile> files);

    DocumentResponseDTO getDocument(UUID userId, UUID documentId);

    void deleteDocument(UUID userId, UUID documentId);

    byte[] downloadDocument(UUID userId, UUID documentId);

    void deleteDocuments(UUID userId, List<UUID> documentIds);
}
