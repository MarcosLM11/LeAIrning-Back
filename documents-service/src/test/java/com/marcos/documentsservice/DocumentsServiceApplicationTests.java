package com.marcos.documentsservice;

import com.marcos.documentsservice.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.ai.vectorstore.VectorStore;

@Import({TestcontainersConfiguration.class, JpaAuditingConfiguration.class})
@SpringBootTest(properties = {
        "spring.ai.vectorstore.qdrant.initialize-schema=false",
        "spring.cloud.function.definition="
})
class DocumentsServiceApplicationTests {

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context can be loaded successfully
        // We mock VectorStore since Ollama is not required for basic context loading
    }

}
