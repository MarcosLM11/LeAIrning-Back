package com.marcos.leairning.etl;

import com.marcos.leairning.documents.DocumentStatus;
import com.marcos.leairning.documents.DocumentsRepository;
import com.marcos.leairning.exception.DocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List   ;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VectorStoreWriterService {

    private final VectorStore vectorStore;
    private final DocumentsRepository documentRepository;

    @Transactional
    public void markProcessing(UUID documentId) {
        updateStatus(documentId, DocumentStatus.PROCESSING);
    }

    @Transactional
    public void write(UUID documentId, List<Document> chunks) {
        vectorStore.add(chunks);
        var document = documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
        document.setStatus(DocumentStatus.PROCESSED);
        documentRepository.save(document);
    }

    @Transactional
    public void markFailed(UUID documentId) {
        updateStatus(documentId, DocumentStatus.FAILED);
    }

    private void updateStatus(UUID documentId, DocumentStatus status) {
        var document = documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
        document.setStatus(status);
        documentRepository.save(document);
    }
}