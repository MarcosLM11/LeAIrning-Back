package com.marcos.leairning.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = JwtSecretProperties.PREFIX)
public class JwtSecretProperties {

    public static final String PREFIX = "leairning.secret.jwt";

    private String value;
    private String algorithm;
}
