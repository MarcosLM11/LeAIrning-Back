package com.marcos.documentsservice.service;

import com.marcos.documentsservice.entity.Document;
import com.marcos.documentsservice.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentProcessorServiceImpl implements DocumentProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentProcessorServiceImpl.class);

    private final StorageService storageService;

    @Override
    @Async
    public void processDocumentAsync(Document document) {
        try {
            var processingPath = storageService.copyToProcessing(document.getStoragePath(), document.getId());
            LOGGER.info("Document {} queued for processing at: {}", document.getId(), processingPath);
        } catch (Exception e) {
            LOGGER.error("Error queueing document for processing: {}", document.getId(), e);
        }
    }
}