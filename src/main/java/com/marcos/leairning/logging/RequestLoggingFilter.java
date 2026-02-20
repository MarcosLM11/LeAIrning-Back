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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Flogger
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String ACTUATOR_PREFIX = "/actuator";
    private static final String ERROR_PATH = "/error";
    private static final Set<String> SENSITIVE_PARAMS = Set.of("code", "token");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var startTime = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            var durationMs = (System.nanoTime() - startTime) / 1_000_000;
            var userId = extractUserId();
            var query = redactQuery(request.getQueryString());
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
