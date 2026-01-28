package com.marcos.leairning.documents;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface DocumentsService {

    void upload(List<MultipartFile> files);
    DocumentResponseDTO getDocument(UUID id);
    void deleteDocument(UUID id);
}
