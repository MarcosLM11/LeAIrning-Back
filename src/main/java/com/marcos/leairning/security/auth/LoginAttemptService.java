package com.marcos.leairning.security.auth;

public interface LoginAttemptService {

    void recordFailedAttempt(String email);

    void resetAttempts(String email);

    boolean isLocked(String email);
}
