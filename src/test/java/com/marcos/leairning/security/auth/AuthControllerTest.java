package com.marcos.leairning.security.auth;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    AuthService authService;
    AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        controller = new AuthController(authService);
    }

    @Test
    void login_returnsOkWithAuthCode() {
        val request = new LoginRequestDTO("test@example.com", "password123!");
        when(authService.login(request)).thenReturn("abc123");
        val response = controller.login(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("abc123", response.getBody().authCode());
        verify(authService).login(request);
    }

    @Test
    void register_returnsAccepted() {
        val request = new RegisterRequestDTO("test@example.com", "Test User", null, "USER", "password123!");
        val response = controller.register(request);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
        verify(authService).register(request);
    }

    @Test
    void verify_returnsOkWithAuthCode() {
        when(authService.verify("token123")).thenReturn("code456");
        val response = controller.verify("token123");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("code456", response.getBody().authCode());
        verify(authService).verify("token123");
    }
}
