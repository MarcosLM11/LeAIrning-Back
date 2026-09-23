package com.marcos.leairning.etl.reader;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DocumentReaderFactory {

    private final List<DocumentReaderStrategy> specificStrategies;
    private final TikaDocumentReaderStrategy tikaFallback;

    public DocumentReaderFactory(List<DocumentReaderStrategy> strategies, TikaDocumentReaderStrategy tikaFallback) {
        this.tikaFallback = tikaFallback;
        this.specificStrategies = strategies.stream()
                .filter(s -> !(s instanceof TikaDocumentReaderStrategy))
                .toList();
    }

    public DocumentReaderStrategy get(String contentType) {
        return specificStrategies.stream()
                .filter(s -> s.supports(contentType))
                .findFirst()
                .orElse(tikaFallback);
    }
}