package com.marcos.leairning.etl.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PdfDocumentReaderStrategy implements DocumentReaderStrategy {

    @Override
    public boolean supports(String contentType) {
        return "application/pdf".equals(contentType);
    }

    @Override
    public List<Document> read(Resource resource) {
        return new PagePdfDocumentReader(resource).read();
    }
}