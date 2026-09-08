package com.marcos.leairning.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String ACTUATOR_PREFIX = "/actuator";
    private static final String ERROR_PATH = "/error";
    private static final Set<String> SENSITIVE_PARAMS = Set.of("code", "token");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var startTime = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            var durationMs = (System.nanoTime() - startTime) / 1_000_000;
            var userId = extractUserId();
            var query = redactQuery(request.getQueryString());
            var uri = query != null ? request.getRequestURI() + "?" + query : request.getRequestURI();
            log.info("{} {} {} {}ms userId={}", request.getMethod(), uri, response.getStatus(), durationMs, userId);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var uri = request.getRequestURI();
        return uri.startsWith(ACTUATOR_PREFIX) || uri.equals(ERROR_PATH);
    }

    String redactQuery(String queryString) {
        if (queryString == null) {
            return null;
        }
        return Arrays.stream(queryString.split("&"))
                .map(param -> {
                    var key = param.split("=")[0];
                    return SENSITIVE_PARAMS.contains(key) ? key + "=***" : param;
                })
                .collect(Collectors.joining("&"));
    }

    private String extractUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return "anonymous";
    }
}
