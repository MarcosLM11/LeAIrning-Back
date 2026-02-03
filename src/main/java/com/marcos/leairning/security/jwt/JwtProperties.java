package com.marcos.leairning.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;

@Data
@Validated
@ConfigurationProperties(prefix = JwtProperties.PREFIX)
public class JwtProperties {

    public static final String PREFIX = "leairning.jwt";

    private Duration accessTokenTtl = ofHours(1);
    private Duration refreshTokenTtl = ofDays(30);

}
