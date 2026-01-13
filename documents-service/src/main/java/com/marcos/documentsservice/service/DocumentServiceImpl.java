package com.marcos.documentsservice.service;

import com.marcos.documentsservice.entity.Document;
import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import com.marcos.documentsservice.entity.dto.DocumentDTO;
import com.marcos.documentsservice.exception.DocumentNotFoundException;
import com.marcos.documentsservice.exception.UnauthorizedAccessException;
import com.marcos.documentsservice.repository.DocumentRepository;
import com.marcos.documentsservice.storage.FileStorageService;
import com.marcos.documentsservice.util.DocumentMapper;
import com.marcos.documentsservice.validator.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DocumentServiceImpl implements DocumentService {

    DocumentRepository documentRepository;
    FileStorageService fileStorageService;
    DocumentMapper documentMapper;
    FileValidator fileValidator;
    DocumentProcessorService documentProcessorService;

    @Override
    @Transactional
    public List<DocumentDTO> uploadDocuments(List<MultipartFile> files, Long userId) {
        return files.stream()
                .map(file -> uploadSingleDocument(file, userId))
                .collect(Collectors.toList());
    }

    private DocumentDTO uploadSingleDocument(MultipartFile file, Long userId) {
        fileValidator.validate(file);

        String storagePath = fileStorageService.store(file, userId);

        Document document = new Document();
        document.setUserId(userId);
        document.setOriginalFilename(file.getOriginalFilename());
        document.setStoredFilename(extractFilename(storagePath));
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setDocumentType(determineDocumentType(file.getContentType()));
        document.setStatus(ProcessingStatus.UPLOADED);
        document.setStoragePath(storagePath);

        Document savedDocument = documentRepository.save(document);
        documentProcessorService.processDocumentAsync(savedDocument);

        return documentMapper.toDTO(savedDocument);
    }

    private String extractFilename(String storagePath) {
        int lastSlash = storagePath.lastIndexOf('/');
        return lastSlash >= 0 ? storagePath.substring(lastSlash + 1) : storagePath;
    }

    private DocumentType determineDocumentType(String contentType) {
        if (contentType == null) {
            return DocumentType.TXT;
        }

        return switch (contentType) {
            case "application/pdf" -> DocumentType.PDF;
            case "text/plain" -> DocumentType.TXT;
            case "text/csv" -> DocumentType.CSV;
            case "application/msword" -> DocumentType.DOC;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DocumentType.DOCX;
            case "text/markdown" -> DocumentType.MARKDOWN;
            default -> DocumentType.TXT;
        };
    }

    @Override
    public DocumentDTO getDocumentById(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        validateOwnership(document, userId);

        return documentMapper.toDTO(document);
    }

    @Override
    public Page<DocumentDTO> getUserDocuments(Long userId, Pageable pageable) {
        Page<Document> documents = documentRepository.findByUserId(userId, pageable);
        return documents.map(documentMapper::toDTO);
    }

    @Override
    public Page<DocumentDTO> getUserDocumentsByStatus(Long userId, ProcessingStatus status, Pageable pageable) {
        Page<Document> documents = documentRepository.findByUserIdAndStatus(userId, status, pageable);
        return documents.map(documentMapper::toDTO);
    }

    @Override
    public Page<DocumentDTO> getUserDocumentsByType(Long userId, DocumentType type, Pageable pageable) {
        Page<Document> documents = documentRepository.findByUserIdAndDocumentType(userId, type, pageable);
        return documents.map(documentMapper::toDTO);
    }

    @Override
    public byte[] downloadDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        validateOwnership(document, userId);

        return fileStorageService.load(document.getStoragePath());
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        validateOwnership(document, userId);

        fileStorageService.delete(document.getStoragePath());
        documentRepository.delete(document);
    }

    @Override
    @Transactional
    public void deleteDocuments(List<Long> documentIds, Long userId) {
        for (Long documentId : documentIds) {
            deleteDocument(documentId, userId);
        }
    }

    @Override
    public long getUserDocumentCount(Long userId) {
        return documentRepository.countByUserId(userId);
    }

    @Override
    public long getUserStorageUsed(Long userId) {
        Page<Document> documents = documentRepository.findByUserId(userId, Pageable.unpaged());
        return documents.stream()
                .mapToLong(Document::getFileSize)
                .sum();
    }

    public Document getDocumentForProcessing(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
    }

    private void validateOwnership(Document document, Long userId) {
        if (!document.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied");
        }
    }
}