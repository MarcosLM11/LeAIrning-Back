package com.marcos.documentsservice.service;

import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import com.marcos.documentsservice.entity.dto.DocumentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    List<DocumentDTO> uploadDocuments(List<MultipartFile> files, Long userId);

    DocumentDTO getDocumentById(Long documentId, Long userId);

    Page<DocumentDTO> getUserDocuments(Long userId, Pageable pageable);

    Page<DocumentDTO> getUserDocumentsByStatus(Long userId, ProcessingStatus status, Pageable pageable);

    Page<DocumentDTO> getUserDocumentsByType(Long userId, DocumentType type, Pageable pageable);

    byte[] downloadDocument(Long documentId, Long userId);

    void deleteDocument(Long documentId, Long userId);

    void deleteDocuments(List<Long> documentIds, Long userId);

    long getUserDocumentCount(Long userId);

    long getUserStorageUsed(Long userId);
}