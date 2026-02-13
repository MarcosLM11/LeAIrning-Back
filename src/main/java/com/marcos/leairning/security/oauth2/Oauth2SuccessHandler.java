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
import lombok.extern.flogger.Flogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles successful OAuth2 authentication from multiple providers (Google, GitHub).
 * Supports provider-agnostic user creation and retrieval.
 */
@Flogger
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Oauth2SuccessHandler extends AuthCodeAuthenticationSuccessHandler {

    private static final String EMAIL = "email";
    private static final String LOGIN = "login";

    UsersService usersService;
    UsersMapper usersMapper;
    GitHubEmailService gitHubEmailService;
    OAuth2AuthorizedClientService authorizedClientService;

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
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            handleOauth2Authentication(request, response, oauthToken);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void handleOauth2Authentication(HttpServletRequest request, HttpServletResponse response, 
                                            OAuth2AuthenticationToken oauthToken) throws IOException {
        val principal = oauthToken.getPrincipal();
        val provider = oauthToken.getAuthorizedClientRegistrationId();
        val email = extractEmail(oauthToken, principal, provider);
        
        log.atInfo().log("Processing OAuth2 login for provider: %s, email: %s", provider, email);
        
        // Search by email + provider (allows same email across different providers)
        val user = usersService.getByEmailAndProvider(email, provider)
                .orElseGet(() -> {
                    log.atInfo().log("Creating new OAuth2 user for provider: %s", provider);
                    val dto = usersMapper.toOauth2CreateDTO(principal, provider);
                    return usersService.saveOauth2User(dto);
                });
                
        val tokenPair = generateTokenPair(user);
        val code = storeAndGetAuthCode(tokenPair);
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
        // Google and others: email comes in the attributes
        return (String) principal.getAttribute(EMAIL);
    }
    
    /**
     * Extracts email from GitHub OAuth2 user.
     * First tries to get public email from attributes.
     * If not available, fetches from GitHub API using the access token.
     * Falls back to username@users.noreply.github.com if no email available.
     */
    private String extractGitHubEmail(OAuth2AuthenticationToken oauthToken, OAuth2User principal) {
        // Try to get email from attributes first (if it's public)
        String email = (String) principal.getAttribute(EMAIL);
        if (email != null && !email.isBlank()) {
            log.atFine().log("Using public email from GitHub attributes");
            return email;
        }
        
        // If not public, get the access token and call /user/emails
        log.atFine().log("Public email not available, fetching from GitHub API");
        val clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
        val principalName = oauthToken.getName();
        val authorizedClient = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId, principalName);
            
        if (authorizedClient == null) {
            log.atSevere().log("Authorized client not found for GitHub");
            throw new IllegalStateException("Authorized client not found");
        }
        
        val accessToken = authorizedClient.getAccessToken().getTokenValue();
        String fetchedEmail = gitHubEmailService.getPrimaryEmail(accessToken);
        
        // If we couldn't fetch email (403 - permission denied), use fallback
        if (fetchedEmail == null) {
            String username = (String) principal.getAttribute(LOGIN);
            if (username != null && !username.isBlank()) {
                fetchedEmail = username + "@users.noreply.github.com";
                log.atWarning().log("Email permission not granted. Using fallback email: %s", fetchedEmail);
            } else {
                throw new IllegalStateException("Cannot extract email or username from GitHub account");
            }
        }
        
        return fetchedEmail;
    }
}
