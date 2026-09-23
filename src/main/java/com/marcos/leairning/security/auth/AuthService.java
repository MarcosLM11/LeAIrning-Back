package com.marcos.leairning.security.auth;

import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import com.github.benmanes.caffeine.cache.Cache;
import com.marcos.leairning.email.EmailService;
import com.marcos.leairning.exception.AccountLockedException;
import com.marcos.leairning.exception.AccountNotVerifiedException;
import com.marcos.leairning.exception.EmailAlreadyRegisteredException;
import com.marcos.leairning.exception.InvalidCredentialsException;
import com.marcos.leairning.exception.InvalidRefreshTokenException;
import com.marcos.leairning.exception.InvalidVerificationTokenException;
import com.marcos.leairning.exception.UserNotFoundException;
import com.marcos.leairning.security.jwt.JwtProperties;
import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.refreshtoken.RefreshToken;
import com.marcos.leairning.security.refreshtoken.RefreshTokenRepository;
import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.User;
import com.marcos.leairning.users.UserRole;
import com.marcos.leairning.users.UsersRepository;
import com.marcos.leairning.users.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RateLimiting(name = "strict")
@RequiredArgsConstructor
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsersService usersService;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenPairService tokenPairService;
    private final Cache<String, String> verificationTokenCache;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public TokenPair login(LoginRequestDTO request) {
        if (loginAttemptService.isLocked(request.email())) {
            throw new AccountLockedException();
        }
        try {
            var user = usersService.getEntityByEmail(request.email());
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                loginAttemptService.recordFailedAttempt(request.email());
                throw new InvalidCredentialsException();
            }
            if (!user.isVerified()) {
                throw new AccountNotVerifiedException();
            }
            loginAttemptService.resetAttempts(request.email());
            return issueTokenPair(user);
        } catch (UserNotFoundException e) {
            loginAttemptService.recordFailedAttempt(request.email());
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public void register(RegisterRequestDTO request) {
        if (usersRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyRegisteredException(request.email());
        }
        var user = User.builder()
                .email(request.email())
                .username(request.name())
                .pictureUrl(request.pictureUrl())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.ROLE_USER)
                .verified(false)
                .provider("local")
                .build();
        usersRepository.save(user);
        val verificationToken = UUID.randomUUID().toString();
        verificationTokenCache.put(verificationToken, request.email());
        emailService.sendVerifyEmail(request.email(), verificationToken);
    }

    @Transactional
    public String verify(String token) {
        val email = verificationTokenCache.getIfPresent(token);
        if (email == null) {
            throw new InvalidVerificationTokenException();
        }
        verificationTokenCache.invalidate(token);

        val user = usersService.updateVerifiedStatus(email);
        val authCode = tokenPairService.add(issueTokenPair(user));

        try {
            emailService.sendWelcomeEmail(email, "Welcome to LeAIrning!");
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}", email);
        }
        return authCode;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPair refresh(RefreshRequestDTO request) {
        var tokenHash = hash(request.refreshToken());
        var refreshToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.isRevoked()) {
            log.warn("Refresh token reuse detected for user {} — revoking all active refresh tokens", refreshToken.getUser().getId());
            refreshTokenRepository.revokeAllActiveByUserId(refreshToken.getUser().getId());
            throw new InvalidRefreshTokenException();
        }
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return issueTokenPair(refreshToken.getUser());
    }

    @Transactional
    public void logout(RefreshRequestDTO request) {
        var tokenHash = hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDTO request) {
        var user = usersService.getEntityById(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) throw new InvalidCredentialsException();
        usersService.updatePassword(userId, passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllActiveByUserId(userId);
    }

    public TokenPair issueTokenPair(User user) {
        var accessToken = jwtService.generateAccessToken(user);
        var rawRefreshToken = generateOpaqueToken();
        var refreshToken = new RefreshToken(null, user, hash(rawRefreshToken), Instant.now().plus(jwtProperties.getRefreshTokenTtl()), false);
        refreshTokenRepository.save(refreshToken);
        return new TokenPair(accessToken, rawRefreshToken);
    }

    private static String generateOpaqueToken() {
        var bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}