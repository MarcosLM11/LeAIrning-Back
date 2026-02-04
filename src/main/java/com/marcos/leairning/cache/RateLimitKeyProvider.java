package com.marcos.leairning.cache;

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

    public String getIp() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return extractIp(servletRequestAttributes.getRequest());
        }
        return UNKNOWN;
    }

    private String extractIp(HttpServletRequest request) {
        var ip = request.getHeader(X_FORWARDED_FOR);
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For puede contener múltiples IPs, tomar la primera
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : UNKNOWN;
    }
}
