package com.marcos.usersservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class RoleValidationFilter extends OncePerRequestFilter {

    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final Pattern USERS_LIST_PATTERN = Pattern.compile("^/api/[^/]+/users$");
    private static final Pattern USER_DELETE_PATTERN = Pattern.compile("^/api/[^/]+/users/\\d+$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var uri = request.getRequestURI();
        var method = request.getMethod();
        if (uri.startsWith("/internal/")) {
            chain.doFilter(request, response);
            return;
        }
        if ("GET".equals(method) && USERS_LIST_PATTERN.matcher(uri).matches()) {
            if (!isAdmin(request)) {
                sendForbidden(response);
                return;
            }
        }
        if ("DELETE".equals(method) && USER_DELETE_PATTERN.matcher(uri).matches()) {
            if (!isAdmin(request)) {
                sendForbidden(response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAdmin(HttpServletRequest request) {
        var role = request.getHeader(USER_ROLE_HEADER);
        return ADMIN_ROLE.equals(role);
    }

    private void sendForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Access denied.\"}");
    }
}