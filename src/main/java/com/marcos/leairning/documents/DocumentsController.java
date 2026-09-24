package com.marcos.leairning.documents;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents/{userId}")
public class DocumentsController {

    private final DocumentsService service;

    public DocumentsController(DocumentsService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDTO> upload(@PathVariable UUID userId, @RequestParam("file") MultipartFile file) {
        var response = service.upload(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getDocuments(@PathVariable UUID userId) {
        var response = service.getDocuments(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentResponseDTO> getDocument(@PathVariable UUID userId, @PathVariable UUID documentId) {
        var response = service.getDocument(userId, documentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID userId, @PathVariable UUID documentId) throws IOException {
        var document = service.downloadDocument(userId, documentId);
        var contentDisposition = ContentDisposition.attachment().filename(document.getFileName()).build();
        return ResponseEntity.ok()
                .contentLength(document.getSize())
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new ByteArrayResource(document.getContent()));
    }

    @DeleteMapping("/{documentId}")
    public void deleteDocument(@PathVariable UUID userId, @PathVariable UUID documentId) {
        service.deleteDocument(userId, documentId);
    }
}