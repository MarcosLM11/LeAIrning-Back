package com.marcos.leairning.documents;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class DocumentsServiceImpl implements DocumentsService {

    DocumentsRepository repository;

    @Override
    public void upload(List<MultipartFile> files) {
        files.forEach(this::uploadDocument);
    }

    private void uploadDocument(MultipartFile file) {
        val document = new Document();
        document.setFileName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSize(file.getSize());
        document.setStoragePath(file.getOriginalFilename());
        repository.save(document);
    }
}
