package com.marcos.leairning.security.jwt;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = JwtSecretProperties.PREFIX)
public class JwtSecretProperties {

    public static final String PREFIX = "leairning.secret.jwt";
    private String value;
    private String algorithm;
}
