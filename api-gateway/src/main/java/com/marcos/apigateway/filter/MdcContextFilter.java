package com.marcos.apigateway.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcContextFilter implements WebFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        var userId = request.getHeaders().getFirst(USER_ID_HEADER);
        var requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-" + UUID.randomUUID().toString().substring(0, 8);
        }
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);
        var finalRequestId = requestId;
        return chain.filter(exchange)
            .contextWrite(ctx -> {
                if (userId != null && !userId.isBlank()) {
                    MDC.put("userId", userId);
                }
                MDC.put("requestId", finalRequestId);
                return ctx;
            })
            .doFinally(signalType -> {
                MDC.remove("userId");
                MDC.remove("requestId");
            });
    }
}
