package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.security.code.AuthCodeAuthenticationSuccessHandler;
import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPairService;
import com.marcos.leairning.users.UsersMapper;
import com.marcos.leairning.users.UsersService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class Oauth2SuccessHandler extends AuthCodeAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(Oauth2SuccessHandler.class);
    private static final String EMAIL = "email";
    private static final String LOGIN = "login";

    private final UsersService usersService;
    private final UsersMapper usersMapper;
    private final GitHubEmailService gitHubEmailService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public Oauth2SuccessHandler(
            JwtService jwtService,
            TokenPairService tokenPairService,
            UsersService usersService,
            UsersMapper usersMapper,
            GitHubEmailService gitHubEmailService,
            OAuth2AuthorizedClientService authorizedClientService,
            @Value("${leairning.auth.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        super(jwtService, tokenPairService, frontendUrl);
        this.usersService = usersService;
        this.usersMapper = usersMapper;
        this.gitHubEmailService = gitHubEmailService;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            handleOauth2Authentication(request, response, oauthToken);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void handleOauth2Authentication(HttpServletRequest request, HttpServletResponse response, 
                                            OAuth2AuthenticationToken oauthToken) throws IOException {
        var principal = oauthToken.getPrincipal();
        var provider = oauthToken.getAuthorizedClientRegistrationId();
        var email = extractEmail(oauthToken, principal, provider);
        
        log.info("Processing OAuth2 login for provider: {}, email: {}", provider, email);
        
        // Search by email + provider (allows same email across different providers)
        var user = usersService.getByEmailAndProvider(email, provider)
                .orElseGet(() -> {
                    log.info("Creating new OAuth2 user for provider: {}", provider);
                    var dto = usersMapper.toOauth2CreateDTO(principal, provider);
                    return usersService.saveOauth2User(dto);
                });
                
        var tokenPair = generateTokenPair(user);
        var code = storeAndGetAuthCode(tokenPair);
        sendAuthCodeRedirect(request, response, code);
    }
    
    /**
     * Extracts email from OAuth2 user based on the provider.
     * For GitHub, if email is not public, fetches it from the /user/emails endpoint.
     * Falls back to username@users.noreply.github.com if email permission not granted.
     */
    private String extractEmail(OAuth2AuthenticationToken oauthToken, 
                                OAuth2User principal, 
                                String provider) {
        if ("github".equalsIgnoreCase(provider)) {
            return extractGitHubEmail(oauthToken, principal);
        }
        return principal.getAttribute(EMAIL);
    }
    
    /**
     * Extracts email from GitHub OAuth2 user.
     * First tries to get public email from attributes.
     * If not available, fetches from GitHub API using the access token.
     * Falls back to username@users.noreply.github.com if no email available.
     */
    private String extractGitHubEmail(OAuth2AuthenticationToken oauthToken, OAuth2User principal) {
        String email = principal.getAttribute(EMAIL);
        if (email != null && !email.isBlank()) {
            log.info("Using public email from GitHub attributes");
            return email;
        }
        
        // If not public, get the access token and call /user/emails
        log.info("Public email not available, fetching from GitHub API");
        var clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
        var principalName = oauthToken.getName();
        var authorizedClient = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId, principalName);
            
        if (authorizedClient == null) {
            log.warn("Authorized client not found for GitHub");
            throw new IllegalStateException("Authorized client not found");
        }
        
        var accessToken = authorizedClient.getAccessToken().getTokenValue();
        String fetchedEmail = gitHubEmailService.getPrimaryEmail(accessToken);
        
        if (fetchedEmail == null) {
            String username = principal.getAttribute(LOGIN);
            if (username != null && !username.isBlank()) {
                fetchedEmail = username + "@users.noreply.github.com";
                log.warn("Email permission not granted. Using fallback email: {}", fetchedEmail);
            } else {
                throw new IllegalStateException("Cannot extract email or username from GitHub account");
            }
        }
        
        return fetchedEmail;
    }
}
