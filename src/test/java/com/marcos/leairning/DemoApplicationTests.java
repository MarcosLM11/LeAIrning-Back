package com.marcos.leairning;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.qdrant.QdrantContainer;

@SpringBootTest
@Testcontainers
class DemoApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.7.4");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Qdrant
        registry.add("spring.ai.vectorstore.qdrant.host", qdrant::getHost);
        registry.add("spring.ai.vectorstore.qdrant.port", qdrant::getGrpcPort);

        // MinIO
        registry.add("minio.endpoint", minio::getS3URL);
        registry.add("minio.access-key", minio::getUserName);
        registry.add("minio.secret-key", minio::getPassword);

        // Disable Ollama for context load test (or use a mock)
        registry.add("spring.ai.ollama.base-url", () -> "http://localhost:11434");
    }

    @Test
    void contextLoads() {
    }

}
