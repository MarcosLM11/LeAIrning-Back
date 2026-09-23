package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.security.auth.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        var redirectUri = UriComponentsBuilder.fromUriString(authProperties.getFrontendUrl())
                .path("/auth/exchange")
                .queryParam("error", "oauth2_failed")
                .build()
                .toUriString();
        response.sendRedirect(redirectUri);
    }
}