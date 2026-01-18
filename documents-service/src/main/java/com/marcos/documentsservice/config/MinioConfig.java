package com.marcos.documentsservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.access-key}")
    private String accessKey;

    @Value("${storage.minio.secret-key}")
    private String secretKey;

    @Value("${storage.minio.documents-bucket}")
    private String documentsBucket;

    @Value("${storage.minio.processing-bucket}")
    private String processingBucket;

    @Value("${storage.minio.auto-create-buckets:true}")
    private boolean autoCreateBuckets;

    @Bean
    public MinioClient minioClient() {
        var client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        if (autoCreateBuckets) {
            createBucketIfNotExists(client, documentsBucket);
            createBucketIfNotExists(client, processingBucket);
        }
        return client;
    }

    private void createBucketIfNotExists(MinioClient client, String bucketName) {
        try {
            var exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                LOGGER.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to initialize MinIO bucket: " + bucketName, e);
        }
    }
}