package com.marcos.documentsservice.storage;

import java.io.InputStream;
import java.util.List;

public interface StorageService {

    String store(byte[] content, String filename, String contentType, Long userId);

    byte[] load(String storagePath);

    InputStream loadAsStream(String storagePath);

    void delete(String storagePath);

    String copyToProcessing(String storagePath, Long documentId);

    List<String> listPendingFiles();

    byte[] loadFromProcessing(String processingPath);

    void markProcessed(String processingPath, boolean success);
}