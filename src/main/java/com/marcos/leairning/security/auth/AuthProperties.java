package com.marcos.leairning.security.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = AuthProperties.PREFIX)
public class AuthProperties {

    public static final String PREFIX = "leairning.auth";

    private String frontendUrl = "http://localhost:4200";
    private Duration verificationTokenTtl = Duration.ofHours(24);
    private List<String> trustedProxies = List.of();
}