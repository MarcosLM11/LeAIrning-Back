package com.marcos.leairning.security.auth;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.util.List;

@Getter
@Validated
@ConfigurationProperties(prefix = AuthProperties.PREFIX)
public class AuthProperties {

    public static final String PREFIX = "leairning.auth";
    private final String frontendUrl = "http://localhost:4200";
    private final Duration verificationTokenTtl = Duration.ofHours(24);
    private final List<String> trustedProxies = List.of();
}