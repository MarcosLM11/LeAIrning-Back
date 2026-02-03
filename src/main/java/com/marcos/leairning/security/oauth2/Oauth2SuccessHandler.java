package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.security.code.AuthCodeAuthenticationSuccessHandler;
import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.UsersMapper;
import com.marcos.leairning.users.UsersService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Oauth2SuccessHandler extends AuthCodeAuthenticationSuccessHandler {

    private static final String EMAIL = "email";

    UsersService usersService;
    UsersMapper usersMapper;

    public Oauth2SuccessHandler(
            JwtService jwtService,
            TokenPairService tokenPairService,
            UsersService usersService,
            UsersMapper usersMapper,
            @Value("${leairning.auth.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        super(jwtService, tokenPairService, frontendUrl);
        this.usersService = usersService;
        this.usersMapper = usersMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            handleOauth2Authentication(request, response, oauthToken);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void handleOauth2Authentication(HttpServletRequest request, HttpServletResponse response, OAuth2AuthenticationToken oauthToken) throws IOException {
        val principal = oauthToken.getPrincipal();
        val email = (String) principal.getAttribute(EMAIL);
        val user = usersService.getByEmail(email).orElseGet(
                () -> {
                    val dto = usersMapper.toOauth2CreateDTO(principal);
                    return usersService.saveOauth2User(dto);
                }
        );
        val tokenPair = generateTokenPair(user);
        val code = storeAndGetAuthCode(tokenPair);
        sendAuthCodeRedirect(request, response, code);
    }
}