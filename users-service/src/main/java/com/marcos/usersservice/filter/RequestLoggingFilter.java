package com.marcos.usersservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var startTime = System.currentTimeMillis();
        var method = request.getMethod();
        var uri = request.getRequestURI();
        var queryString = request.getQueryString();
        var fullPath = queryString != null ? uri + "?" + queryString : uri;
        try {
            filterChain.doFilter(request, response);
        } finally {
            var duration = System.currentTimeMillis() - startTime;
            var status = response.getStatus();
            if (status >= 500) {
                log.error("HTTP {} {} - {} ({}ms)", method, fullPath, status, duration);
            } else if (status >= 400) {
                log.warn("HTTP {} {} - {} ({}ms)", method, fullPath, status, duration);
            } else {
                log.info("HTTP {} {} - {} ({}ms)", method, fullPath, status, duration);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return path.startsWith("/actuator");
    }
}
