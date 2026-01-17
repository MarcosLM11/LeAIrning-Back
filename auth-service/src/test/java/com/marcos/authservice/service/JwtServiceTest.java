package com.marcos.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-minimum-32-characters-long";
    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, Duration.ofHours(1), Duration.ofDays(7));
        var secretKey = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Test
    @DisplayName("Should generate valid access token with correct claims")
    void shouldGenerateValidAccessTokenWithCorrectClaims() {
        // Given
        var userId = 123L;
        var username = "john.doe";
        var role = "USER";

        // When
        var token = jwtService.generateAccessToken(userId, username, role);

        // Then
        assertThat(token).isNotBlank();
        var jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("123");
        assertThat((String) jwt.getClaim("username")).isEqualTo(username);
        assertThat((String) jwt.getClaim("role")).isEqualTo(role);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("leairning-auth");
    }

    @Test
    @DisplayName("Should generate random refresh token")
    void shouldGenerateRandomRefreshToken() {
        // When
        var token1 = jwtService.generateRefreshToken();
        var token2 = jwtService.generateRefreshToken();

        // Then
        assertThat(token1).isNotBlank();
        assertThat(token2).isNotBlank();
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should hash refresh token consistently")
    void shouldHashRefreshTokenConsistently() {
        // Given
        var token = "test-refresh-token";

        // When
        var hash1 = jwtService.hashRefreshToken(token);
        var hash2 = jwtService.hashRefreshToken(token);

        // Then
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 produces 64 hex characters
    }

    @Test
    @DisplayName("Should return correct access token TTL in seconds")
    void shouldReturnCorrectAccessTokenTtlInSeconds() {
        // When
        var ttl = jwtService.getAccessTokenTtlSeconds();

        // Then
        assertThat(ttl).isEqualTo(3600L); // 1 hour in seconds
    }
}
