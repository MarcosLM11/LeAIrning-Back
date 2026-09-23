package com.marcos.leairning.security.auth;

import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import com.github.benmanes.caffeine.cache.Cache;
import com.marcos.leairning.email.EmailService;
import com.marcos.leairning.exception.AccountLockedException;
import com.marcos.leairning.exception.AccountNotVerifiedException;
import com.marcos.leairning.exception.InvalidCredentialsException;
import com.marcos.leairning.exception.InvalidVerificationTokenException;
import com.marcos.leairning.exception.UserNotFoundException;
import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.users.User;
import com.marcos.leairning.users.UserResponseDTO;
import com.marcos.leairning.users.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RateLimiting(name = "strict")
@RequiredArgsConstructor
public class AuthService {

    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final Cache<String, String> verificationTokenCache;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public TokenPairResponse login(LoginRequestDTO request) {
        if (loginAttemptService.isLocked(request.email())) {
            throw new AccountLockedException();
        }
        var user = usersService.getEntityByEmail(request.email());
        var matches = passwordEncoder.matches(request.password(), user.getPassword());

        if (!matches) {
            log.warn("Login failed for identifier '{}'", request.email());
            throw new InvalidCredentialsException();
        }
        return issueTokenPair(user.get());
    }

    @Transactional
    public void register(RegisterRequestDTO request) {
        usersService.save(request);
        val verificationToken = UUID.randomUUID().toString();
        verificationTokenCache.put(verificationToken, request.email());
        emailService.sendVerificationEmail(request.email(), verificationToken);
    }

    @Transactional
    public String verify(String token) {
        val email = verificationTokenCache.getIfPresent(token);
        
        if (email == null) {
            throw new InvalidVerificationTokenException();
        }
        
        verificationTokenCache.invalidate(token);

        val user = usersService.updateVerifiedStatus(email);
        val authCode = generateAuthCode(user);

        try {
            emailService.sendWelcomeEmail(email, "Welcome to LeAIrning!");
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}", email);
        }

        return authCode;
    }

    public void logout(UUID userId) {
        revokedTokenService.revokeAllForUser(userId);
        log.info("All tokens revoked for user: {}", userId);
    }

    private String generateAuthCode(UserResponseDTO user) {
        val accessToken = jwtService.generateAccessToken(user);
        val refreshToken = jwtService.generateRefreshToken(user);
        val tokenPair = new TokenPair(accessToken, refreshToken);
        return tokenPairService.add(tokenPair);
    }
}