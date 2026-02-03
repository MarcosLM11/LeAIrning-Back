package com.marcos.leairning.security.jwt;

import com.marcos.leairning.users.UserResponseDTO;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    JwtEncoder encoder;
    JwtProperties jwtProperties;
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        encoder = mock(JwtEncoder.class);
        jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenTtl(Duration.ofMinutes(30));
        jwtProperties.setRefreshTokenTtl(Duration.ofDays(7));
        jwtService = new JwtService(encoder, jwtProperties);
    }

    @Test
    void generateAccessToken_shouldEncodeWithBusinessScope() {
        var user = createUser();
        var mockJwt = mock(Jwt.class);
        
        when(mockJwt.getTokenValue()).thenReturn("access-token-value");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);
        
        val token = jwtService.generateAccessToken(user);
        
        assertEquals("access-token-value", token);
        
        val captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        
        verify(encoder).encode(captor.capture());
        
        val claims = captor.getValue().getClaims();
        
        assertEquals("business", claims.getClaim("scope"));
        assertEquals(user.id().toString(), claims.getSubject());
        assertEquals("self", claims.getClaimAsString("iss"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiresAt());
    }

    @Test
    void generateRefreshToken_shouldEncodeWithRefreshTokenScope() {
        var user = createUser();
        var mockJwt = mock(Jwt.class);
        
        when(mockJwt.getTokenValue()).thenReturn("refresh-token-value");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);
        
        val token = jwtService.generateRefreshToken(user);
        
        assertEquals("refresh-token-value", token);
        
        val captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        
        verify(encoder).encode(captor.capture());
        
        val claims = captor.getValue().getClaims();
        
        assertEquals("refresh-token", claims.getClaim("scope"));
    }

    @Test
    void rotateFromJwt_shouldGenerateNewTokenPairFromClaims() {
        var jwt = mock(Jwt.class);
        
        when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("USER"));
        
        var mockEncodedJwt = mock(org.springframework.security.oauth2.jwt.Jwt.class);
        
        when(mockEncodedJwt.getTokenValue()).thenReturn("new-token");
        when(encoder.encode(any())).thenReturn(mockEncodedJwt);
        
        val tokenPair = jwtService.rotateFromJwt(jwt);
        
        assertNotNull(tokenPair);
        assertEquals("new-token", tokenPair.accessToken());
        assertEquals("new-token", tokenPair.refreshToken());
        verify(encoder, times(2)).encode(any());
    }

    @Test
    void generateAccessToken_shouldIncludeRolesInClaims() {
        var user = createUser();
        var mockJwt = mock(Jwt.class);
        
        when(mockJwt.getTokenValue()).thenReturn("token");
        when(encoder.encode(any())).thenReturn(mockJwt);
        
        jwtService.generateAccessToken(user);
        val captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        
        verify(encoder).encode(captor.capture());
        
        val claims = captor.getValue().getClaims();
        val roles = claims.getClaim("roles");
        
        assertNotNull(roles);
        assertEquals(List.of("USER"), roles);
    }

    private UserResponseDTO createUser() {
        return UserResponseDTO.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role("USER")
                .name("Test")
                .verified(true)
                .build();
    }
}