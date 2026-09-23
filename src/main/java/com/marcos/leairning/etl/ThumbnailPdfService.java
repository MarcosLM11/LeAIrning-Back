package com.marcos.leairning.etl;

import com.marcos.leairning.documents.DocumentsRepository;
import com.marcos.leairning.exception.DocumentNotFoundException;
import com.marcos.leairning.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailPdfService {

    private static final String SUPPORTED_CONTENT_TYPE = "application/pdf";
    private static final int RENDER_DPI = 96;
    private final MinioService minioService;
    private final DocumentsRepository documentRepository;
    
    @Transactional
    public void generate(UUID documentId, String contentType, Resource resource) {
        if (!SUPPORTED_CONTENT_TYPE.equals(contentType)) return;

        try (var pdf = Loader.loadPDF(resource.getContentAsByteArray())) {
            var image = new PDFRenderer(pdf).renderImageWithDPI(0, RENDER_DPI, ImageType.RGB);
            var buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            var document = documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
            minioService.upload(document.getThumbnailPath(), new ByteArrayInputStream(buffer.toByteArray()));
            log.debug("Generated thumbnail for document {}", documentId);
        } catch (Exception e) {
            log.warn("Could not generate thumbnail for document {}", documentId, e);
        }
    }
}