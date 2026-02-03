package com.marcos.leairning.security.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.marcos.leairning.email.EmailService;
import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.*;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    UsersService usersService;
    UsersMapper mapper;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;
    TokenPairService tokenPairService;
    EmailService emailService;
    Cache<String, String> verificationTokenCache;
    AuthServiceImpl authService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        usersService = mock(UsersService.class);
        mapper = mock(UsersMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        tokenPairService = mock(TokenPairService.class);
        emailService = mock(EmailService.class);
        verificationTokenCache = mock(Cache.class);
        authService = new AuthServiceImpl(
                usersService, mapper, passwordEncoder, jwtService,
                tokenPairService, emailService, verificationTokenCache
        );
    }

    @Test
    void login_withValidCredentials_returnsAuthCode() {
        var entity = createVerifiedEntity();
        var dto = createVerifiedUser();
        
        when(usersService.getEntityByEmail("test@example.com")).thenReturn(entity);
        when(passwordEncoder.matches("password123!", entity.getPassword())).thenReturn(true);
        when(mapper.toResponse(entity)).thenReturn(dto);
        when(jwtService.generateAccessToken(dto)).thenReturn("access");
        when(jwtService.generateRefreshToken(dto)).thenReturn("refresh");
        when(tokenPairService.add(any())).thenReturn("auth-code");
        
        val code = authService.login(new LoginRequestDTO("test@example.com", "password123!"));
        
        assertEquals("auth-code", code);
    }

    @Test
    void login_withInvalidPassword_throws() {
        var entity = createVerifiedEntity();
        
        when(usersService.getEntityByEmail("test@example.com")).thenReturn(entity);
        when(passwordEncoder.matches("wrong", entity.getPassword())).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequestDTO("test@example.com", "wrong")));
    }

    @Test
    void login_withUnverifiedAccount_throws() {
        var entity = createVerifiedEntity();
        entity.setVerified(false);
        
        when(usersService.getEntityByEmail("test@example.com")).thenReturn(entity);
        when(passwordEncoder.matches("password123!", entity.getPassword())).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequestDTO("test@example.com", "password123!")));
    }

    @Test
    void login_withNonexistentEmail_throws() {
        when(usersService.getEntityByEmail("none@example.com"))
                .thenThrow(new IllegalArgumentException("Unable to find user"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequestDTO("none@example.com", "password123!")));
    }

    @Test
    void register_savesUserAndSendsEmail() {
        var request = new RegisterRequestDTO("new@example.com", "Test User", null, "USER", "password12345");
        authService.register(request);
        
        verify(usersService).save(request);
        verify(emailService).sendVerificationEmail(eq("new@example.com"), any());
        verify(verificationTokenCache).put(any(), eq("new@example.com"));
    }

    @Test
    void verify_withValidToken_verifiesUserAndReturnsAuthCode() {
        var dto = createVerifiedUser();
        
        when(verificationTokenCache.getIfPresent("valid-token")).thenReturn("test@example.com");
        when(usersService.updateVerifiedStatus("test@example.com")).thenReturn(dto);
        when(jwtService.generateAccessToken(dto)).thenReturn("access");
        when(jwtService.generateRefreshToken(dto)).thenReturn("refresh");
        when(tokenPairService.add(any())).thenReturn("auth-code");
        
        val code = authService.verify("valid-token");
        
        assertEquals("auth-code", code);
        verify(verificationTokenCache).invalidate("valid-token");
        verify(usersService).updateVerifiedStatus("test@example.com");
        verify(emailService).sendWelcomeEmail("test@example.com", "Welcome to LeAIrning!");
    }

    @Test
    void verify_withInvalidToken_throws() {
        when(verificationTokenCache.getIfPresent("bad-token")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> authService.verify("bad-token"));
    }

    private User createVerifiedEntity() {
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        user.setRole("USER");
        user.setVerified(true);
        return user;
    }

    private UserResponseDTO createVerifiedUser() {
        return UserResponseDTO.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .role("USER")
                .verified(true)
                .build();
    }
}