package com.marcos.leairning.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RevokedTokenService {

    Cache<String, Instant> revokedTokensCache;

    public void revokeAllForUser(UUID userId) {
        revokedTokensCache.put(userId.toString(), Instant.now());
    }

    public boolean isRevoked(String userId, Instant issuedAt) {
        var revokedAt = revokedTokensCache.getIfPresent(userId);
        return revokedAt != null && issuedAt.isBefore(revokedAt);
    }
}
