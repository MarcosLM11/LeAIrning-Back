package com.marcos.leairning.documents;

import com.marcos.leairning.web.CurrentUserId;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class DocumentsController {

    DocumentsService service;

    @GetMapping
    public Page<DocumentResponseDTO> getDocuments(
            @CurrentUserId UUID userId,
            @PageableDefault(sort = "createdTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return service.getDocuments(userId, pageable);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<DocumentResponseDTO> upload(
            @CurrentUserId UUID userId,
            @RequestParam("files") List<MultipartFile> files) {

        return service.upload(userId, files);
    }

    @GetMapping("/{documentId}")
    public DocumentResponseDTO getDocument(
            @CurrentUserId UUID userId,
            @PathVariable UUID documentId) {

        return service.getDocument(userId, documentId);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @CurrentUserId UUID userId,
            @PathVariable UUID documentId) {

        val document = service.getDocument(userId, documentId);
        val content = service.downloadDocument(userId, documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.fileName() + "\"")
                .contentType(MediaType.parseMediaType(document.contentType()))
                .contentLength(content.length)
                .body(content);
    }

    @DeleteMapping("/{documentId}")
    public void deleteDocument(
            @CurrentUserId UUID userId,
            @PathVariable UUID documentId) {

        service.deleteDocument(userId, documentId);
    }

    @DeleteMapping("/batch")
    public void deleteDocuments(
            @CurrentUserId UUID userId,
            @RequestBody List<UUID> documentIds) {

        service.deleteDocuments(userId, documentIds);
    }

}
