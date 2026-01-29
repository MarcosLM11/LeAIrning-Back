package com.marcos.leairning.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioConfig {



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
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create or check bucket: " + bucketName, e);
        }
    }
}
