package com.marcos.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        var path = request.getPath().value();
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        var startTime = System.currentTimeMillis();
        var method = request.getMethod().name();
        var query = request.getURI().getQuery();
        var fullPath = query != null ? path + "?" + query : path;
        return chain.filter(exchange)
            .doFinally(signalType -> {
                var duration = System.currentTimeMillis() - startTime;
                var response = exchange.getResponse();
                var status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
                if (status >= 500) {
                    log.error("HTTP {} {} - {} ({}ms)", method, fullPath, status, duration);
                } else if (status >= 400) {
                    log.warn("HTTP {} {} - {} ({}ms)", method, fullPath, status, duration);
                } else {
                    log.info("HTTP {} {} - {} ({}ms)", method, fullPath, status, duration);
                }
            });
    }
}
