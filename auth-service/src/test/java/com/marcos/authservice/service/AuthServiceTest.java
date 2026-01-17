package com.marcos.authservice.service;

import com.marcos.authservice.client.UserClient;
import com.marcos.authservice.dto.InternalUserDTO;
import com.marcos.authservice.entity.RefreshToken;
import com.marcos.authservice.exception.InvalidCredentialsException;
import com.marcos.authservice.exception.InvalidRefreshTokenException;
import com.marcos.authservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserClient userClient;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userClient, jwtService, refreshTokenRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Given
        var username = "john.doe";
        var password = "password123";
        var hashedPassword = "$2a$10$hashedPassword";
        var user = new InternalUserDTO(1L, username, hashedPassword, "USER");
        when(userClient.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);
        when(jwtService.generateAccessToken(1L, username, "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtService.hashRefreshToken("refresh-token")).thenReturn("hashed-refresh");
        when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(3600));
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // When
        var result = authService.login(username, password);

        // Then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.expiresIn()).isEqualTo(3600L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userClient.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login("unknown", "password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void shouldThrowExceptionWhenPasswordIsIncorrect() {
        // Given
        var user = new InternalUserDTO(1L, "john", "$2a$10$hash", "USER");
        when(userClient.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login("john", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should refresh token with rotation")
    void shouldRefreshTokenWithRotation() {
        // Given
        var oldToken = "old-refresh-token";
        var oldTokenHash = "old-hash";
        var storedToken = new RefreshToken(1L, oldTokenHash, Instant.now().plusSeconds(3600));
        when(jwtService.hashRefreshToken(oldToken)).thenReturn(oldTokenHash);
        when(refreshTokenRepository.findByTokenHash(oldTokenHash)).thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(1L, null, null)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtService.hashRefreshToken("new-refresh-token")).thenReturn("new-hash");
        when(jwtService.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(3600));
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);

        // When
        var result = authService.refresh(oldToken);

        // Then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).deleteByTokenHash(oldTokenHash);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when refresh token not found")
    void shouldThrowExceptionWhenRefreshTokenNotFound() {
        // Given
        when(jwtService.hashRefreshToken("invalid")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refresh("invalid"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Should throw exception when refresh token expired")
    void shouldThrowExceptionWhenRefreshTokenExpired() {
        // Given
        var expiredToken = new RefreshToken(1L, "hash", Instant.now().minusSeconds(3600));
        when(jwtService.hashRefreshToken("expired")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> authService.refresh("expired"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
