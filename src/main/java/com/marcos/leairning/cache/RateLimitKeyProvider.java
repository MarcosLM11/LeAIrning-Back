package com.marcos.leairning.cache;

import com.marcos.leairning.security.auth.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component("rateLimitKeyProvider")
@RequiredArgsConstructor
public class RateLimitKeyProvider {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String UNKNOWN = "unknown";

    private final AuthProperties authProperties;

    public String getIp() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return extractIp(servletRequestAttributes.getRequest());
        }
        return UNKNOWN;
    }

    private String extractIp(HttpServletRequest request) {
        var remoteAddr = request.getRemoteAddr();
        if (authProperties.getTrustedProxies().contains(remoteAddr)) {
            var forwarded = request.getHeader(X_FORWARDED_FOR);
            if (forwarded != null && !forwarded.isEmpty() && !UNKNOWN.equalsIgnoreCase(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr != null ? remoteAddr : UNKNOWN;
    }
}
