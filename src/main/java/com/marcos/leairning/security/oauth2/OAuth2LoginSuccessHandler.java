package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.security.auth.AuthProperties;
import com.marcos.leairning.security.auth.AuthService;
import com.marcos.leairning.security.token.TokenPairService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final AuthService authService;
    private final TokenPairService tokenPairService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(AuthService authService, TokenPairService tokenPairService, AuthProperties authProperties) {
        this.authService = authService;
        this.tokenPairService = tokenPairService;
        this.frontendUrl = authProperties.getFrontendUrl();
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication authentication) throws IOException, ServletException {
        var user = ((LocalUserPrincipal) authentication.getPrincipal()).getUser();
        var tokenPair = authService.issueTokenPair(user);
        var code = tokenPairService.add(tokenPair);
        var redirectUri = fromUriString(frontendUrl).path("/auth/exchange").queryParam("code", code).toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}