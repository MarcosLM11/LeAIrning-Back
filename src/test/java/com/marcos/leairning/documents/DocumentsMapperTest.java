package com.marcos.leairning.documents;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentsMapperTest {

    DocumentsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DocumentsMapperImpl();
    }

    @Test
    void toEntity_mapsMultipartFileFields() {
        val file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(2048L);
        val entity = mapper.toEntity(file);
        assertEquals("application/pdf", entity.getContentType());
        assertEquals(2048L, entity.getSize());
    }

    @Test
    void toEntity_withNullFile_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDTO_mapsAllFields() {
        val doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setUserId(UUID.randomUUID());
        doc.setFileName("test.pdf");
        doc.setContentType("application/pdf");
        doc.setSize(1024L);
        doc.setStoragePath("/bucket/test.pdf");
        val dto = mapper.toDTO(doc);
        assertEquals(doc.getId(), dto.id());
        assertEquals(doc.getUserId(), dto.userId());
        assertEquals("test.pdf", dto.fileName());
        assertEquals("application/pdf", dto.contentType());
        assertEquals(1024L, dto.size());
        assertEquals("/bucket/test.pdf", dto.storagePath());
    }

    @Test
    void toDTO_withNullDocument_returnsNull() {
        assertNull(mapper.toDTO(null));
    }
}
