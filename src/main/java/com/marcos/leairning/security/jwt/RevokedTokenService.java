package com.marcos.leairning.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class RevokedTokenService {

    private final Cache<String, Instant> revokedTokensCache;

    public RevokedTokenService(Cache<String, Instant> revokedTokensCache) {
        this.revokedTokensCache = revokedTokensCache;
    }

    public void revokeAllForUser(UUID userId) {
        revokedTokensCache.put(userId.toString(), Instant.now());
    }

    public boolean isRevoked(String userId, Instant issuedAt) {
        var revokedAt = revokedTokensCache.getIfPresent(userId);
        return revokedAt != null && issuedAt.isBefore(revokedAt);
    }
}
