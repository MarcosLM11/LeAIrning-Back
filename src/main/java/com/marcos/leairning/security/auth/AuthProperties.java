package com.marcos.leairning.security.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = AuthProperties.PREFIX)
public class AuthProperties {
    public static final String PREFIX = "leairning.auth";
    private String frontendUrl = "http://localhost:4200";
    private List<String> trustedProxies = List.of();
}