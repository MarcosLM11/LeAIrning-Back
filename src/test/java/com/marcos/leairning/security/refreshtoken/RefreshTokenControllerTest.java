package com.marcos.leairning.security.refreshtoken;

import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPair;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenControllerTest {

    JwtService jwtService;
    RefreshTokenController controller;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        controller = new RefreshTokenController(jwtService);
    }

    @Test
    void refresh_returnsOkWithTokenPair() {
        val jwt = mock(Jwt.class);
        val pair = new TokenPair("new-access", "new-refresh");
        when(jwtService.rotateFromJwt(jwt)).thenReturn(pair);
        val response = controller.refresh(jwt);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pair, response.getBody());
        verify(jwtService).rotateFromJwt(jwt);
    }
}
