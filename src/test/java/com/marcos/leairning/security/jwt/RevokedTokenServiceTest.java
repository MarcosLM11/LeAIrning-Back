package com.marcos.leairning.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class RevokedTokenServiceTest {

    Cache<String, Instant> cache;
    RevokedTokenService revokedTokenService;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
        revokedTokenService = new RevokedTokenService(cache);
    }

    @Test
    void isRevoked_returnsFalse_whenNoRevocation() {
        val userId = UUID.randomUUID();
        assertFalse(revokedTokenService.isRevoked(userId.toString(), Instant.now()));
    }

    @Test
    void isRevoked_returnsTrue_whenTokenIssuedBeforeRevocation() {
        val userId = UUID.randomUUID();
        val issuedAt = Instant.now().minusSeconds(60);
        revokedTokenService.revokeAllForUser(userId);
        assertTrue(revokedTokenService.isRevoked(userId.toString(), issuedAt));
    }

    @Test
    void isRevoked_returnsFalse_whenTokenIssuedAfterRevocation() {
        val userId = UUID.randomUUID();
        revokedTokenService.revokeAllForUser(userId);
        val issuedAt = Instant.now().plusSeconds(1);
        assertFalse(revokedTokenService.isRevoked(userId.toString(), issuedAt));
    }

    @Test
    void revokeAllForUser_updatesRevocationTime() {
        val userId = UUID.randomUUID();
        val firstIssuedAt = Instant.now().minusSeconds(60);
        revokedTokenService.revokeAllForUser(userId);
        assertTrue(revokedTokenService.isRevoked(userId.toString(), firstIssuedAt));
    }
}
