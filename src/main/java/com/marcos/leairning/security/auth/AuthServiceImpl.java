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
import com.marcos.leairning.security.jwt.RevokedTokenService;
import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.UserResponseDTO;
import com.marcos.leairning.users.UsersMapper;
import com.marcos.leairning.users.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RateLimiting(name = "strict")
@RequiredArgsConstructor()
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UsersService usersService;
    private final UsersMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RevokedTokenService revokedTokenService;
    private final TokenPairService tokenPairService;
    private final EmailService emailService;
    private final Cache<String, String> verificationTokenCache;
    private final LoginAttemptService loginAttemptService;

    @Override
    public String login(LoginRequestDTO request) {
        if (loginAttemptService.isLocked(request.email())) {
            throw new AccountLockedException();
        }
        try {
            val user = usersService.getEntityByEmail(request.email());
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                loginAttemptService.recordFailedAttempt(request.email());
                throw new InvalidCredentialsException();
            }
            if (!user.isVerified()) {
                throw new AccountNotVerifiedException();
            }
            loginAttemptService.resetAttempts(request.email());
            return generateAuthCode(mapper.toResponse(user));
        } catch (UserNotFoundException e) {
            loginAttemptService.recordFailedAttempt(request.email());
            throw new InvalidCredentialsException();
        }
    }

    @Override
    @Transactional
    public void register(RegisterRequestDTO request) {
        usersService.save(request);
        val verificationToken = UUID.randomUUID().toString();
        verificationTokenCache.put(verificationToken, request.email());
        emailService.sendVerificationEmail(request.email(), verificationToken);
    }

    @Override
    @Transactional
    public String verify(String token) {
        val email = verificationTokenCache.getIfPresent(token);
        
        if (email == null) {
            throw new InvalidVerificationTokenException();
        }
        
        verificationTokenCache.invalidate(token);

        val user = usersService.updateVerifiedStatus(email);
        val authCode = generateAuthCode(user);

        // Send welcome email - don't let it break the verification flow
        try {
            emailService.sendWelcomeEmail(email, "Welcome to LeAIrning!");
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}", email);
        }

        return authCode;
    }

    @Override
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