package com.marcos.leairning.minio;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Configuration
public class MinioConfig {
    private static final String BUCKET_NAME = "leairning";
    private final S3Client s3Client;

    public MinioConfig(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Bean
    ApplicationRunner createBucket() {
        return _ -> {
            try {
                s3Client.headBucket( r -> r.bucket(BUCKET_NAME) );
            }  catch (NoSuchBucketException _) {
                s3Client.createBucket( r -> r.bucket(BUCKET_NAME) );
            }
        };
    }
}
