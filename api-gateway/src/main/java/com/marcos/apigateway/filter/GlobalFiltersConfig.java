package com.marcos.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalFiltersConfig extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GlobalFiltersConfig.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Generate X-Request-Id if not present
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Wrap request to add X-Request-Id header for downstream services
        final String finalRequestId = requestId;
        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                    return finalRequestId;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(finalRequestId));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                if (!names.contains(REQUEST_ID_HEADER)) {
                    names.add(REQUEST_ID_HEADER);
                }
                return Collections.enumeration(names);
            }
        };

        // Log incoming request
        String method = request.getMethod();
        String path = request.getRequestURI();
        String userId = request.getHeader(USER_ID_HEADER);

        log.info(">>> [{}] {} {} - User: {}",
                requestId, method, path, userId != null ? userId : "anonymous");

        // Add X-Request-Id to response for client traceability
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< [{}] {} {} - Status: {} - Duration: {}ms",
                    requestId, method, path, response.getStatus(), duration);
        }
    }
}