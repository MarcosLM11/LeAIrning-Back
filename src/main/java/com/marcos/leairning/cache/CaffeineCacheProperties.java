package com.marcos.leairning.cache;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

import static java.time.Duration.ofSeconds;

@Data
@Validated
@ConfigurationProperties(prefix = CaffeineCacheProperties.PREFIX)
public class CaffeineCacheProperties {

    public static final String PREFIX = "leairning.cache.caffeine";
    public static final String DEFAULT_POLICY = "default";
    public static final String STRICT_POLICY = "strict";
    public static final String LENIENT_POLICY = "lenient";

    @Positive
    private int documentsMaximumSize = 1000;
    private Duration documentsExpireAfterAccess = Duration.ofSeconds(60);

    @Positive
    private int usersMaximumSize = 1000;
    private Duration usersExpireAfterAccess = Duration.ofSeconds(60);

    @Positive
    private int rateLimitMaximumSize = 100_000;
    private Duration rateLimitExpireAfterAccess = Duration.ofHours(1);
}
