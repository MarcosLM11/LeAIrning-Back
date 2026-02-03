package com.marcos.leairning.web;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserIdArgumentResolverTest {

    CurrentUserIdArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserIdArgumentResolver();
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsParameter_withCurrentUserIdAndUUID_returnsTrue() {
        val parameter = mock(MethodParameter.class);
        
        when(parameter.hasParameterAnnotation(CurrentUserId.class)).thenReturn(true);
        when(parameter.getParameterType()).thenReturn((Class) UUID.class);
        assertTrue(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_withoutAnnotation_returnsFalse() {
        val parameter = mock(MethodParameter.class);
        
        when(parameter.hasParameterAnnotation(CurrentUserId.class)).thenReturn(false);
        when(parameter.getParameterType()).thenReturn((Class) UUID.class);
        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_withWrongType_returnsFalse() {
        val parameter = mock(MethodParameter.class);
        
        when(parameter.hasParameterAnnotation(CurrentUserId.class)).thenReturn(true);
        when(parameter.getParameterType()).thenReturn((Class) String.class);
        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void resolveArgument_withJwtAuthentication_returnsUserId() {
        val userId = UUID.randomUUID();
        
        val jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "HS512"), Map.of("sub", userId.toString()));
        
        val auth = new AbstractAuthenticationToken(Collections.emptyList()) {
            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return jwt;
            }
        };
        
        auth.setAuthenticated(true);
        val context = mock(SecurityContext.class);
        
        when(context.getAuthentication()).thenReturn(auth);
        
        SecurityContextHolder.setContext(context);
        val resolved = resolver.resolveArgument(null, null, null, null);
        
        assertEquals(userId, resolved);
    }

    @Test
    void resolveArgument_withNoAuthentication_returnsNull() {
        val context = mock(SecurityContext.class);
        
        when(context.getAuthentication()).thenReturn(null);
        
        SecurityContextHolder.setContext(context);
        val resolved = resolver.resolveArgument(null, null, null, null);
        
        assertNull(resolved);
    }
}