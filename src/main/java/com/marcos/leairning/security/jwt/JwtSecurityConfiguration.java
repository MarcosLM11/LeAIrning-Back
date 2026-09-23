package com.marcos.leairning.security.jwt;

import com.marcos.leairning.security.AbstractSecurityConfiguration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.SneakyThrows;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;
import java.util.stream.Collectors;
import static org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withSecretKey;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, JwtSecretProperties.class})
public class JwtSecurityConfiguration extends AbstractSecurityConfiguration {

    private static final String JWT_SECURITY_FILTER_CHAIN = "jwtSecurityFilterChain";

    private static final String[] SECURED_PATTERNS = {
            "/auth/logout",
            "/api/**",
            "/users/**",
            "/documents/**",
            "/conversations/**",
            "/chat/**",
            "/quizz/**"
    };

    private final JwtSecretProperties properties;

    public JwtSecurityConfiguration(JwtSecretProperties properties) {
        this.properties = properties;
    }

    @SneakyThrows
    @Order(HIGEST_PRECEDENCE + 4_000)
    @Bean(JWT_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        http.securityMatcher(SECURED_PATTERNS)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                );
        return buildWithDefaults(http);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var roles = jwt.getClaimAsStringList(JwtService.ROLES);
            if (roles == null) {
                return List.of();
            }
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    @Bean
    JwtEncoder jwtEncoder() {
        var bytes = properties.getValue().getBytes();
        return new NimbusJwtEncoder(new ImmutableSecret<>(bytes));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        var bytes = properties.getValue().getBytes();
        var algorithm = properties.getAlgorithm();
        var key = new SecretKeySpec(bytes, 0, bytes.length, algorithm);
        return withSecretKey(key).macAlgorithm(MacAlgorithm.valueOf(algorithm)).build();
    }
}