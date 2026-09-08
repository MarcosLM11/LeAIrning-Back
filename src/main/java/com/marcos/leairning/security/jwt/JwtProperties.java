package com.marcos.leairning.security.jwt;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;

@Getter
@Validated
@ConfigurationProperties(prefix = JwtProperties.PREFIX)
public class JwtProperties {

    public static final String PREFIX = "leairning.jwt";
    private final Duration accessTokenTtl = ofHours(1);
    private final Duration refreshTokenTtl = ofDays(30);

}
