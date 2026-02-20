package com.marcos.leairning.cache;

import com.marcos.leairning.security.auth.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitKeyProviderTest {

    AuthProperties authProperties;
    RateLimitKeyProvider provider;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        provider = new RateLimitKeyProvider(authProperties);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getIp_returnsRemoteAddr_whenNoForwardedHeader() {
        var request = mockRequestWithIp("192.168.1.100", null);
        setRequest(request);
        assertEquals("192.168.1.100", provider.getIp());
    }

    @Test
    void getIp_ignoresForwardedFor_whenRemoteAddrNotTrusted() {
        var request = mockRequestWithIp("10.0.0.5", "8.8.8.8");
        setRequest(request);
        assertEquals("10.0.0.5", provider.getIp());
    }

    @Test
    void getIp_usesForwardedFor_whenRemoteAddrIsTrustedProxy() {
        authProperties.setTrustedProxies(List.of("10.0.0.1"));
        var request = mockRequestWithIp("10.0.0.1", "203.0.113.50");
        setRequest(request);
        assertEquals("203.0.113.50", provider.getIp());
    }

    @Test
    void getIp_usesFirstIp_fromMultipleForwardedFor() {
        authProperties.setTrustedProxies(List.of("10.0.0.1"));
        var request = mockRequestWithIp("10.0.0.1", "203.0.113.50, 10.0.0.2");
        setRequest(request);
        assertEquals("203.0.113.50", provider.getIp());
    }

    @Test
    void getIp_returnsRemoteAddr_whenForwardedForIsEmpty() {
        authProperties.setTrustedProxies(List.of("10.0.0.1"));
        var request = mockRequestWithIp("10.0.0.1", "");
        setRequest(request);
        assertEquals("10.0.0.1", provider.getIp());
    }

    @Test
    void getIp_returnsUnknown_whenNoRequestContext() {
        assertEquals("unknown", provider.getIp());
    }

    private HttpServletRequest mockRequestWithIp(String remoteAddr, String forwardedFor) {
        val request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        return request;
    }

    private void setRequest(HttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
