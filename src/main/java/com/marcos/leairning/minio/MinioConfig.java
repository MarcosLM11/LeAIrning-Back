package com.marcos.leairning.minio;

import com.marcos.leairning.exception.StorageBucketInitializationException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private final MinioProperties properties;

    public MinioConfig(MinioProperties properties) {
        this.properties = properties;
    }

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
            throw new StorageBucketInitializationException(bucketName, e);
        }
    }
}
