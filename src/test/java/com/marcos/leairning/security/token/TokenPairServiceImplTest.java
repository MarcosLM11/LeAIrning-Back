package com.marcos.leairning.security.token;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenPairServiceImplTest {

    TokenPairServiceImpl service;

    @BeforeEach
    void setUp() {
        var cache = Caffeine.newBuilder()
                .maximumSize(100)
                .<String, TokenPair>build();
        service = new TokenPairServiceImpl(cache);
    }

    @Test
    void add_shouldReturnCode() {
        val pair = new TokenPair("access", "refresh");
        val code = service.add(pair);
        
        assertNotNull(code);
        assertFalse(code.isBlank());
    }

    @Test
    void find_afterAdd_shouldReturnPair() {
        val pair = new TokenPair("access", "refresh");
        val code = service.add(pair);
        val found = service.find(code);
        
        assertTrue(found.isPresent());
        assertEquals("access", found.get().accessToken());
        assertEquals("refresh", found.get().refreshToken());
    }

    @Test
    void find_withUnknownCode_shouldReturnEmpty() {
        val found = service.find("nonexistent");
        
        assertTrue(found.isEmpty());
    }

    @Test
    void remove_shouldInvalidateCode() {
        val pair = new TokenPair("access", "refresh");
        val code = service.add(pair);
        service.remove(code);
        val found = service.find(code);
        
        assertTrue(found.isEmpty());
    }
}