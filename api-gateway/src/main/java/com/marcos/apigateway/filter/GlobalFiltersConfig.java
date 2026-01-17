package com.marcos.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
        var startTime = System.currentTimeMillis();
        var request = exchange.getRequest();
        var response = exchange.getResponse();
        var method = request.getMethod().name();
        var path = request.getURI().getPath();
        var requestId = getOrGenerateRequestId(request);
        response.getHeaders().add(REQUEST_ID_HEADER, requestId);
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(jwt -> jwt.getToken().getSubject())
                .defaultIfEmpty("anonymous")
                .flatMap(userId -> {
                    log.info(">>> [{}] {} {} - User: {}", requestId, method, path, userId);
                    var mutatedRequest = request.mutate()
                            .header(REQUEST_ID_HEADER, requestId)
                            .headers(h -> h.remove(USER_ID_HEADER))
                            .header(USER_ID_HEADER, userId)
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .then(Mono.fromRunnable(() -> {
                    var duration = System.currentTimeMillis() - startTime;
                    log.info("<<< [{}] {} {} - Status: {} - Duration: {}ms",
                            requestId, method, path,
                            response.getStatusCode() != null ? response.getStatusCode().value() : "unknown",
                            duration);
                }));
    }

    private String getOrGenerateRequestId(ServerHttpRequest request) {
        var requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        return (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;
    }

    @Override
    public int getOrder() {
        return -1; // Run after security filter
    }
}