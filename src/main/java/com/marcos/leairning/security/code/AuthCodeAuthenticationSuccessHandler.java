package com.marcos.leairning.security.code;

import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.User;
import com.marcos.leairning.users.UserResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import java.io.IOException;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthCodeAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    JwtService jwtService;
    TokenPairService tokenPairService;
    String frontendUrl;

    protected TokenPair generateTokenPair(UserResponseDTO user) {
        val accessToken = jwtService.generateAccessToken(user);
        val refreshToken = jwtService.generateRefreshToken(user);

        return new TokenPair(accessToken, refreshToken);
    }

    protected String storeAndGetAuthCode(TokenPair tokenPair) {
        return tokenPairService.add(tokenPair);
    }

    protected void sendAuthCodeRedirect(HttpServletRequest request, HttpServletResponse response, String code) throws IOException {
        val redirectStrategy = getRedirectStrategy();
        val builder = fromUriString(frontendUrl);

        builder.path("/auth/exchange");
        builder.queryParam("code", code);

        val authExchangeUrl = builder.toUriString();

        redirectStrategy.sendRedirect(request, response, authExchangeUrl);
    }
}