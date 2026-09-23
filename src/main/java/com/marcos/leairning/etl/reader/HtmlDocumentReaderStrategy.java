package com.marcos.leairning.etl.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class HtmlDocumentReaderStrategy implements DocumentReaderStrategy {

    @Override
    public boolean supports(String contentType) {
        return "text/html".equals(contentType);
    }

    @Override
    public List<Document> read(Resource resource) {
        return new JsoupDocumentReader(resource).read();
    }
}