package com.marcos.usersservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleValidationFilterTest {

    private RoleValidationFilter filter;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RoleValidationFilter();
    }

    @Test
    void getAllUsers_withAdminRole_shouldProceed() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/1.0/users");
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void getAllUsers_withUserRole_shouldReturn403() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/1.0/users");
        when(request.getHeader("X-User-Role")).thenReturn("USER");
        var writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void getAllUsers_withNoRole_shouldReturn403() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/1.0/users");
        when(request.getHeader("X-User-Role")).thenReturn(null);
        var writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void deleteUser_withAdminRole_shouldProceed() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/1.0/users/123");
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void deleteUser_withUserRole_shouldReturn403() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/1.0/users/123");
        when(request.getHeader("X-User-Role")).thenReturn("USER");
        var writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void getUser_withUserRole_shouldProceed() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/1.0/users/123");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void createUser_withNoRole_shouldProceed() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/1.0/users");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void updateUser_withUserRole_shouldProceed() throws Exception {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/1.0/users/123");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void internalEndpoint_shouldProceed() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/internal/users/by-username/john");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}