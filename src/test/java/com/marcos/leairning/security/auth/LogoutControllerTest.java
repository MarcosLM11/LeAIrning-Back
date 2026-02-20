package com.marcos.leairning.security.auth;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogoutControllerTest {

    AuthService authService;
    LogoutController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        controller = new LogoutController(authService);
    }

    @Test
    void logout_returnsNoContent() {
        val userId = UUID.randomUUID();
        val response = controller.logout(userId);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).logout(userId);
    }
}
