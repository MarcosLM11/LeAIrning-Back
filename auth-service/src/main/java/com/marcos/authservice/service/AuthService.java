package com.marcos.authservice.service;

import com.marcos.authservice.client.UserClient;
import com.marcos.authservice.dto.TokenResponse;
import com.marcos.authservice.entity.RefreshToken;
import com.marcos.authservice.exception.InvalidCredentialsException;
import com.marcos.authservice.exception.InvalidRefreshTokenException;
import com.marcos.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class AuthService {

    UserClient userClient;
    JwtService jwtService;
    RefreshTokenRepository refreshTokenRepository;
    PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResponse login(String username, String password) {
        var user = userClient.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        var accessToken = jwtService.generateAccessToken(user.id(), user.username(), user.role());
        var refreshToken = jwtService.generateRefreshToken();
        saveRefreshToken(user.id(), refreshToken);
        log.info("User {} logged in successfully", username);
        return new TokenResponse(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        var tokenHash = jwtService.hashRefreshToken(refreshTokenValue);
        var storedToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow(InvalidRefreshTokenException::new);
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.deleteByTokenHash(tokenHash);
            throw new InvalidRefreshTokenException();
        }
        refreshTokenRepository.deleteByTokenHash(tokenHash);
        var accessToken = jwtService.generateAccessToken(storedToken.getUserId(), null, null);
        var newRefreshToken = jwtService.generateRefreshToken();
        saveRefreshToken(storedToken.getUserId(), newRefreshToken);
        log.info("Token refreshed for user {}", storedToken.getUserId());
        return new TokenResponse(accessToken, newRefreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        var tokenHash = jwtService.hashRefreshToken(refreshToken);
        var expiry = jwtService.getRefreshTokenExpiry();
        var entity = new RefreshToken(userId, tokenHash, expiry);
        refreshTokenRepository.save(entity);
    }
}
