package com.marcos.leairning.etl;

import com.marcos.leairning.documents.DocumentUploadedEvent;
import com.marcos.leairning.documents.DocumentsRepository;
import com.marcos.leairning.etl.reader.DocumentReaderFactory;
import com.marcos.leairning.exception.DocumentNotFoundException;
import com.marcos.leairning.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentsRepository documentRepository;
    private final MinioService minioService;
    private final DocumentReaderFactory readerFactory;
    private final ChunkingService chunkingService;
    private final VectorStoreWriterService writerService;
    private final ThumbnailPdfService thumbnailPdfService;
    private final EtlMetrics metrics;

    @Async("etlTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        process(event.documentId());
    }

    private void process(UUID documentId) {
        log.info("Starting ETL for document {}", documentId);
        writerService.markProcessing(documentId);
        var sample = metrics.startTimer();
        try {
            var metadata = documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
            var resource = minioService.download(metadata.getStoragePath());
            log.debug("Downloaded document {}", documentId);
            thumbnailPdfService.generate(documentId, metadata.getContentType(), resource);
            var rawDocuments = readerFactory.get(metadata.getContentType()).read(resource);
            log.debug("Read document {}: {} raw document(s), contentType={}", documentId, rawDocuments.size(), metadata.getContentType());
            var chunks = chunkingService.chunk(rawDocuments, documentId, metadata.getUserId(), metadata.getFileName());
            log.debug("Chunked document {}: {} chunks", documentId, chunks.size());
            writerService.write(documentId, chunks);
            log.debug("Wrote {} chunks for document {} to the vector store", chunks.size(), documentId);
            metrics.recordSuccess(sample);
            log.info("ETL completed for document {}: {} chunks", documentId, chunks.size());
        } catch (Exception e) {
            log.error("Error processing document {}", documentId, e);
            writerService.markFailed(documentId);
            metrics.recordFailure(sample);
        }
    }
}