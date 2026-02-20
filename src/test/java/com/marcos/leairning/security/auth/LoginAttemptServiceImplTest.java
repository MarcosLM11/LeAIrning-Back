package com.marcos.leairning.security.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceImplTest {

    LoginAttemptServiceImpl service;
    Cache<String, Integer> cache;
    LoginLockoutProperties properties;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build();
        properties = new LoginLockoutProperties();
        properties.setMaxAttempts(5);
        service = new LoginAttemptServiceImpl(cache, properties);
    }

    @Test
    void isLocked_returnsFalse_withNoAttempts() {
        assertFalse(service.isLocked("user@example.com"));
    }

    @Test
    void isLocked_returnsFalse_belowThreshold() {
        for (int i = 0; i < 4; i++) {
            service.recordFailedAttempt("user@example.com");
        }
        assertFalse(service.isLocked("user@example.com"));
    }

    @Test
    void isLocked_returnsTrue_atThreshold() {
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt("user@example.com");
        }
        assertTrue(service.isLocked("user@example.com"));
    }

    @Test
    void isLocked_returnsTrue_aboveThreshold() {
        for (int i = 0; i < 7; i++) {
            service.recordFailedAttempt("user@example.com");
        }
        assertTrue(service.isLocked("user@example.com"));
    }

    @Test
    void resetAttempts_unlocksAccount() {
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt("user@example.com");
        }
        assertTrue(service.isLocked("user@example.com"));
        service.resetAttempts("user@example.com");
        assertFalse(service.isLocked("user@example.com"));
    }

    @Test
    void recordFailedAttempt_incrementsCount() {
        service.recordFailedAttempt("user@example.com");
        assertEquals(1, cache.getIfPresent("user@example.com"));
        service.recordFailedAttempt("user@example.com");
        assertEquals(2, cache.getIfPresent("user@example.com"));
    }

    @Test
    void attempts_arePerEmail() {
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt("locked@example.com");
        }
        assertTrue(service.isLocked("locked@example.com"));
        assertFalse(service.isLocked("other@example.com"));
    }
}
