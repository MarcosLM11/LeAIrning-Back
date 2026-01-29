package com.marcos.leairning.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = MinioProperties.PREFIX)
public class MinioProperties {

    public static final String PREFIX = "leairning.storage.minio";

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String documentsBucket;

    private String processingBucket;

    private boolean autoCreateBuckets;
}
