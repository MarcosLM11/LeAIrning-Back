package com.marcos.documentsservice.controller;

import com.marcos.documentsservice.entity.DocumentType;
import com.marcos.documentsservice.entity.ProcessingStatus;
import com.marcos.documentsservice.entity.dto.*;
import com.marcos.documentsservice.exception.InvalidRequestException;
import com.marcos.documentsservice.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RestController
@RequestMapping("/api/{version}/documents")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DocumentController {

    DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadDocumentResponse> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @RequestHeader("X-User-Id") Long userId) {
        log.info(">>> Request to upload {} documents for userId={}", files.size(), userId);

        if (files.isEmpty()) {
            throw new InvalidRequestException("No files provided");
        }

        var uploadedDocuments = documentService.uploadDocuments(files, userId);

        var response = new UploadDocumentResponse(
                uploadedDocuments,
                "Documents uploaded successfully. Processing started."
        );

        return ResponseEntity.status(202).body(response);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDTO> getDocumentById(
            @PathVariable Long documentId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info(">>> Request to get document by id={} for userId={}", documentId, userId);
        var document = documentService.getDocumentById(documentId, userId);
        return ResponseEntity.ok(document);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> getUserDocuments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            Pageable pageable
    ) {
        log.info(">>> Request to get documents for userId={} with status={} and type={}", userId, status, type);
        Page<DocumentDTO> documents;

        if (status != null) {
            ProcessingStatus processingStatus = ProcessingStatus.valueOf(status);
            documents = documentService.getUserDocumentsByStatus(userId, processingStatus, pageable);
        } else if (type != null) {
            DocumentType documentType = DocumentType.valueOf(type);
            documents = documentService.getUserDocumentsByType(userId, documentType, pageable);
        } else {
            documents = documentService.getUserDocuments(userId, pageable);
        }

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long documentId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info(">>> Request to download document id={} for userId={}", documentId, userId);
        var fileContent = documentService.downloadDocument(documentId, userId);
        var document = documentService.getDocumentById(documentId, userId);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.contentType()));
        headers.setContentDispositionFormData("attachment", document.originalFilename());

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info(">>> Request to delete document id={} for userId={}", documentId, userId);
        documentService.deleteDocument(documentId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/batch")
    public ResponseEntity<BatchDeleteResponse> deleteDocuments(
            @Valid @RequestBody BatchDeleteRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info(">>> Request to batch delete {} documents for userId={}", request.documentIds().size(), userId);
        documentService.deleteDocuments(request.documentIds(), userId);

        var response = new BatchDeleteResponse(
                request.documentIds().size(),
                0
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics(
            @RequestHeader("X-User-Id") Long userId) {
        log.info(">>> Request to get user statistics for userId={}", userId);
        var totalDocuments = documentService.getUserDocumentCount(userId);
        var storageUsed = documentService.getUserStorageUsed(userId);

        var response = new UserStatisticsResponse(totalDocuments, storageUsed);

        return ResponseEntity.ok(response);
    }
}