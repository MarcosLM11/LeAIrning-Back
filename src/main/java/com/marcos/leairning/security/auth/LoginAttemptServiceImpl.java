package com.marcos.leairning.security.auth;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final Cache<String, Integer> loginAttemptsCache;
    private final LoginLockoutProperties lockoutProperties;

    public LoginAttemptServiceImpl(Cache<String, Integer> loginAttemptsCache,  LoginLockoutProperties lockoutProperties) {
        this.loginAttemptsCache = loginAttemptsCache;
        this.lockoutProperties = lockoutProperties;
    }

    @Override
    public void recordFailedAttempt(String email) {
        var attempts = loginAttemptsCache.get(email, _ -> 0);
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
