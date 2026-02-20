package com.marcos.leairning.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestLoggingFilterTest {

    RequestLoggingFilter filter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_logsRequestWithoutError() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/users");
        response.setStatus(200);
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_propagatesExceptions() throws ServletException, IOException {
        request.setMethod("POST");
        request.setRequestURI("/api/documents");
        doThrow(new ServletException("test error")).when(chain).doFilter(request, response);
        assertThrows(ServletException.class, () -> filter.doFilterInternal(request, response, chain));
    }

    @Test
    void doFilterInternal_includesQueryString() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/users");
        request.setQueryString("page=0&size=20");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_includesAuthenticatedUserId() throws ServletException, IOException {
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
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.setMethod("GET");
        request.setRequestURI("/api/documents");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_handlesUnauthenticatedRequest() throws ServletException, IOException {
        request.setMethod("POST");
        request.setRequestURI("/auth/login");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotFilter_excludesActuatorPaths() {
        request.setRequestURI("/actuator/health");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_excludesErrorPath() {
        request.setRequestURI("/error");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_includesApiPaths() {
        request.setRequestURI("/api/users");
        assertFalse(filter.shouldNotFilter(request));
    }

    @ParameterizedTest
    @CsvSource({
            "code=abc123, code=***",
            "token=secret123, token=***",
            "code=abc&other=value, code=***&other=value",
            "page=0&token=secret&size=20, page=0&token=***&size=20",
            "page=0&size=20, page=0&size=20"
    })
    void redactQuery_redactsSensitiveParams(String input, String expected) {
        assertEquals(expected, filter.redactQuery(input));
    }

    @Test
    void redactQuery_returnsNull_forNullInput() {
        assertNull(filter.redactQuery(null));
    }

    @Test
    void doFilterInternal_redactsSensitiveQueryParams() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/auth/code/exchange");
        request.setQueryString("code=secret-auth-code");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
