package com.marcos.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Component
public class GlobalFiltersConfig implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GlobalFiltersConfig.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();

        // Generate X-Request-Id if not present
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Log incoming request
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String userId = request.getHeaders().getFirst(USER_ID_HEADER);

        log.info(">>> [{}] {} {} - User: {}",
                requestId, method, path, userId != null ? userId : "anonymous");

        // Mutate request to add X-Request-Id header for downstream services
        final String finalRequestId = requestId;
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, finalRequestId)
                .build();

        // Add X-Request-Id to response for client traceability
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(REQUEST_ID_HEADER, finalRequestId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("<<< [{}] {} {} - Status: {} - Duration: {}ms",
                            finalRequestId, method, path,
                            response.getStatusCode() != null ? response.getStatusCode().value() : "unknown",
                            duration);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}