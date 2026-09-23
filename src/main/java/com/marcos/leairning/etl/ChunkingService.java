package com.marcos.leairning.etl;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class ChunkingService {

    public static final String METADATA_OWNER_ID = "ownerId";
    public static final String METADATA_DOCUMENT_ID = "documentId";
    public static final String METADATA_FILE_NAME = "fileName";
    public static final String METADATA_CHUNK_INDEX = "chunkIndex";

    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(400)
            .withMinChunkLengthToEmbed(10)
            .withKeepSeparator(false)
            .build();

    public List<Document> chunk(List<Document> documents, UUID documentId, UUID ownerId, String fileName) {
        var chunks = splitter.apply(documents);
        return IntStream.range(0, chunks.size())
                .mapToObj(i -> enrich(chunks.get(i), i, documentId, ownerId, fileName))
                .toList();
    }

    private Document enrich(Document chunk, int chunkIndex, UUID documentId, UUID ownerId, String fileName) {
        return chunk.mutate()
                .metadata(METADATA_OWNER_ID, ownerId.toString())
                .metadata(METADATA_DOCUMENT_ID, documentId.toString())
                .metadata(METADATA_FILE_NAME, fileName)
                .metadata(METADATA_CHUNK_INDEX, chunkIndex)
                .build();
    }
}