package com.marcos.leairning.security.auth;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginAttemptServiceImpl implements LoginAttemptService {

    Cache<String, Integer> loginAttemptsCache;
    LoginLockoutProperties lockoutProperties;

    @Override
    public void recordFailedAttempt(String email) {
        var attempts = loginAttemptsCache.get(email, k -> 0);
        loginAttemptsCache.put(email, attempts + 1);
    }

    @Override
    public void resetAttempts(String email) {
        loginAttemptsCache.invalidate(email);
    }

    @Override
    public boolean isLocked(String email) {
        var attempts = loginAttemptsCache.getIfPresent(email);
        return attempts != null && attempts >= lockoutProperties.getMaxAttempts();
    }
}
