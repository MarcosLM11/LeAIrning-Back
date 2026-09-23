package com.marcos.leairning.security.auth;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final Cache<String, Integer> loginAttemptsCache;
    private final LoginLockoutProperties lockoutProperties;

    public void recordFailedAttempt(String email) {
        var attempts = loginAttemptsCache.get(email, _ -> 0);
        loginAttemptsCache.put(email, attempts + 1);
    }

    public void resetAttempts(String email) {
        loginAttemptsCache.invalidate(email);
    }

    public boolean isLocked(String email) {
        var attempts = loginAttemptsCache.getIfPresent(email);
        return attempts != null && attempts >= lockoutProperties.getMaxAttempts();
    }
}
