package com.marcos.leairning.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;
import java.util.stream.Collectors;
import static org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withSecretKey;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, JwtSecretProperties.class})
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtSecretProperties properties;

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