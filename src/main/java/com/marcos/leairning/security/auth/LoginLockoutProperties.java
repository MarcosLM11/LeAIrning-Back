package com.marcos.leairning.security.auth;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = LoginLockoutProperties.PREFIX)
public class LoginLockoutProperties {

    public static final String PREFIX = "leairning.security.login-lockout";

    @Positive
    private int maxAttempts = 5;
    private Duration lockoutDuration = Duration.ofMinutes(15);
    @Positive
    private int cacheMaxSize = 100_000;
}
