package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.security.auth.AuthProperties;
import com.marcos.leairning.security.auth.AuthService;
import com.marcos.leairning.security.code.AuthCodeAuthenticationSuccessHandler;
import com.marcos.leairning.security.token.TokenPairService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends AuthCodeAuthenticationSuccessHandler {

    public OAuth2LoginSuccessHandler(AuthService authService, TokenPairService tokenPairService, AuthProperties authProperties) {
        super(authService, tokenPairService, authProperties.getFrontendUrl());
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication authentication) throws IOException, ServletException {
        var user = ((LocalUserPrincipal) authentication.getPrincipal()).getUser();
        var code = issueAuthCode(user);
        sendAuthCodeRedirect(request, response, code);
    }
}