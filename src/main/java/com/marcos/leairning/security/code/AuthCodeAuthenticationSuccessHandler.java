package com.marcos.leairning.security.code;

import com.marcos.leairning.security.auth.AuthService;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import java.io.IOException;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

public class AuthCodeAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final AuthService authService;
    private final TokenPairService tokenPairService;
    private final String frontendUrl;

    public AuthCodeAuthenticationSuccessHandler(AuthService authService, TokenPairService tokenPairService, String frontendUrl) {
        this.authService = authService;
        this.tokenPairService = tokenPairService;
        this.frontendUrl = frontendUrl;
    }

    protected String issueAuthCode(User user) {
        var tokenPair = authService.issueTokenPair(user);
        return tokenPairService.add(tokenPair);
    }

    protected void sendAuthCodeRedirect(HttpServletRequest request, HttpServletResponse response, String code) throws IOException {
        var builder = fromUriString(frontendUrl).path("/auth/exchange").queryParam("code", code);
        getRedirectStrategy().sendRedirect(request, response, builder.toUriString());
    }
}