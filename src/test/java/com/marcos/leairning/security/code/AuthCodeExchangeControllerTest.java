package com.marcos.leairning.security.code;

import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.security.token.TokenPairService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthCodeExchangeControllerTest {

    TokenPairService tokenPairService;
    AuthCodeExchangeController controller;

    @BeforeEach
    void setUp() {
        tokenPairService = mock(TokenPairService.class);
        controller = new AuthCodeExchangeController(tokenPairService);
    }

    @Test
    void exchange_withValidCode_returnsTokenPair() {
        val pair = new TokenPair("access-token", "refresh-token");
        when(tokenPairService.find("valid-code")).thenReturn(Optional.of(pair));
        val result = controller.exchange("valid-code");
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        verify(tokenPairService).remove("valid-code");
    }

    @Test
    void exchange_withInvalidCode_throws401() {
        when(tokenPairService.find("invalid")).thenReturn(Optional.empty());
        val ex = assertThrows(ResponseStatusException.class, () -> controller.exchange("invalid"));
        assertEquals(401, ex.getStatusCode().value());
        verify(tokenPairService, never()).remove(any());
    }
}
