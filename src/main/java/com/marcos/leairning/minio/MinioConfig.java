package com.marcos.leairning.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinioConfig {

    MinioProperties properties;

    @Bean
    public MinioClient minioClient() {
        var client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        if (properties.isAutoCreateBuckets()) {
            createBucketIfNotExists(client, properties.getDocumentsBucket());
            createBucketIfNotExists(client, properties.getProcessingBucket());
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
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create or check bucket: " + bucketName, e);
        }
    }
}
