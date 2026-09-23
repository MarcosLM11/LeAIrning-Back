package com.marcos.leairning.etl.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class TikaDocumentReaderStrategy implements DocumentReaderStrategy {

    @Override
    public boolean supports(String contentType) {
        return true;
    }

    @Override
    public List<Document> read(Resource resource) {
        return new TikaDocumentReader(resource).read();
    }
}