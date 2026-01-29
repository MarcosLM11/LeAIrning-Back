package com.marcos.leairning.minio;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = MinioProperties.PREFIX)
public class MinioProperties {

    public static final String PREFIX = "leairning.storage.minio";

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
}
