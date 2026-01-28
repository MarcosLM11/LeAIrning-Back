package com.marcos.leairning.documents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface DocumentsService {

    Page<DocumentResponseDTO> getDocuments(Pageable pageable);
    List<DocumentResponseDTO> upload(List<MultipartFile> files);
    DocumentResponseDTO getDocument(UUID id);
    void deleteDocument(UUID id);
}
