package com.marcos.leairning.security.jwt;

import com.marcos.leairning.security.AbstractSecurityConfiguration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.spec.SecretKeySpec;
import static org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withSecretKey;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableConfigurationProperties({JwtProperties.class, JwtSecretProperties.class})
public class JwtSecurityConfiguration extends AbstractSecurityConfiguration {

    private static final String JWT_SECURITY_FILTER_CHAIN = "jwtSecurityFilterChain";

    private static final String[] SECURED_PATTERNS = {
            "/token/refresh",
            "/api/**",
            "/users/**"
    };

    JwtSecretProperties properties;

    @SneakyThrows
    @Order(HIGEST_PRECEDENCE + 4_000)
    @Bean(JWT_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) {
        http.securityMatcher(SECURED_PATTERNS)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                );
        return buildWithDefaults(http);
    }

    @Bean
    JwtEncoder jwtEncoder() {
        val secret = properties.getValue();
        val bytes = secret.getBytes();
        val immutableSecret = new ImmutableSecret<>(bytes);

        return new NimbusJwtEncoder(immutableSecret);
    }

    @Bean
    JwtDecoder jwtDecoder() {
        val secret = properties.getValue();
        val bytes = secret.getBytes();
        val algorithm = properties.getAlgorithm();
        val originalKey = new SecretKeySpec(bytes, 0, bytes.length, algorithm);

        return withSecretKey(originalKey).macAlgorithm(MacAlgorithm.valueOf(algorithm)).build();
    }
}