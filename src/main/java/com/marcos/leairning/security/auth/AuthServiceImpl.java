package com.marcos.leairning.security.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.marcos.leairning.email.EmailService;
import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.UserResponseDTO;
import com.marcos.leairning.users.UsersMapper;
import com.marcos.leairning.users.UsersService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Flogger
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {

    UsersService usersService;
    UsersMapper mapper;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;
    TokenPairService tokenPairService;
    EmailService emailService;
    Cache<String, String> verificationTokenCache;

    @Override
    public String login(LoginRequestDTO request) {
        val user = usersService.getEntityByEmail(request.email());
        
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        if (!user.isVerified()) {
            throw new IllegalArgumentException("Account not verified");
        }
        
        return generateAuthCode(mapper.toResponse(user));
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
            throw new IllegalArgumentException("Invalid or expired verification token");
        }
        
        verificationTokenCache.invalidate(token);

        val user = usersService.updateVerifiedStatus(email);
        emailService.sendWelcomeEmail(email, "Welcome to LeAIrning!");
        val authCode = generateAuthCode(user);

        // Send welcome email - don't let it break the verification flow
        try {
            emailService.sendWelcomeEmail(email, "Welcome to LeAIrning!");
        } catch (Exception e) {
            log.atWarning().withCause(e).log("Failed to send welcome email to %s", email);
        }

        return authCode;
    }

    private String generateAuthCode(UserResponseDTO user) {
        val accessToken = jwtService.generateAccessToken(user);
        val refreshToken = jwtService.generateRefreshToken(user);
        val tokenPair = new TokenPair(accessToken, refreshToken);
        return tokenPairService.add(tokenPair);
    }
}