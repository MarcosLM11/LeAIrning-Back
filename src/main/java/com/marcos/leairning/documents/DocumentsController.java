package com.marcos.leairning.documents;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @PageableDefault(sort = "createdTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return service.getDocuments(pageable);
    }

    @PostMapping
    public void upload(@RequestParam("files") List<MultipartFile> files) {
        service.upload(files);
    }

    @GetMapping("/{documentId}")
    public DocumentResponseDTO getDocument(@PathVariable UUID documentId) {
        return service.getDocument(documentId);
    }

    @DeleteMapping("/{documentId}")
    public void deleteDocument(@PathVariable UUID documentId) {
        service.deleteDocument(documentId);
    }

}
