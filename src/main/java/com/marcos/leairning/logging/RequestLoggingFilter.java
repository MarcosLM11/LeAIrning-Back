package com.marcos.leairning.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.flogger.Flogger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Flogger
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String ACTUATOR_PREFIX = "/actuator";
    private static final String ERROR_PATH = "/error";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var startTime = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            var durationMs = (System.nanoTime() - startTime) / 1_000_000;
            var userId = extractUserId();
            var query = request.getQueryString();
            var uri = query != null ? request.getRequestURI() + "?" + query : request.getRequestURI();
            log.atInfo().log("%s %s %d %dms userId=%s",
                    request.getMethod(), uri, response.getStatus(), durationMs, userId);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var uri = request.getRequestURI();
        return uri.startsWith(ACTUATOR_PREFIX) || uri.equals(ERROR_PATH);
    }

    private String extractUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return "anonymous";
    }
}
