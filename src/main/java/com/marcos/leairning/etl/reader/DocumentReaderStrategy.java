package com.marcos.leairning.etl.reader;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import java.util.List;

public interface DocumentReaderStrategy {
    boolean supports(String contentType);
    List<Document> read(Resource resource);
}
