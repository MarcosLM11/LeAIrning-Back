package com.marcos.documentsservice.service;

import com.marcos.documentsservice.entity.Document;

public interface DocumentProcessorService {

    void processDocumentAsync(Document document);
}