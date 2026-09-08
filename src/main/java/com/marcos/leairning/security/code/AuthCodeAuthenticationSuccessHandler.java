package com.marcos.leairning.security.code;

import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.UserResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import java.io.IOException;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

public class AuthCodeAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final TokenPairService tokenPairService;
    private final String frontendUrl;

    public AuthCodeAuthenticationSuccessHandler(JwtService jwtService, TokenPairService tokenPairService, String frontendUrl) {
        this.jwtService = jwtService;
        this.tokenPairService = tokenPairService;
        this.frontendUrl = frontendUrl;
    }

    protected TokenPair generateTokenPair(UserResponseDTO user) {
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        return new TokenPair(accessToken, refreshToken);
    }

    protected String storeAndGetAuthCode(TokenPair tokenPair) {
        return tokenPairService.add(tokenPair);
    }

    protected void sendAuthCodeRedirect(HttpServletRequest request, HttpServletResponse response, String code) throws IOException {
        var redirectStrategy = getRedirectStrategy();
        var builder = fromUriString(frontendUrl);
        builder.path("/auth/exchange");
        builder.queryParam("code", code);
        var authExchangeUrl = builder.toUriString();
        redirectStrategy.sendRedirect(request, response, authExchangeUrl);
    }
}