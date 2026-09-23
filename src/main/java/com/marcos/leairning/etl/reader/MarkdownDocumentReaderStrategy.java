package com.marcos.leairning.etl.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;

@Component
public class MarkdownDocumentReaderStrategy implements DocumentReaderStrategy {

    private static final Set<String> SUPPORTED_TYPES = Set.of("text/markdown", "text/x-markdown");

    @Override
    public boolean supports(String contentType) {
        return SUPPORTED_TYPES.contains(contentType);
    }

    @Override
    public List<Document> read(Resource resource) {
        return new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.defaultConfig()).read();
    }
}