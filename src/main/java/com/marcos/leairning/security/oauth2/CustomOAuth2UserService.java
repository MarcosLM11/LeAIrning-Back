package com.marcos.leairning.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

/**
 * Resolves GitHub users. GitHub only exposes a public email attribute when the
 * user has made one public, so a private email is fetched separately via /user/emails.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String GITHUB_EMAILS_URI = "https://api.github.com/user/emails";
    private final OAuthUserProvisioningService provisioningService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestClient restClient = RestClient.create();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            var oAuth2User = delegate.loadUser(userRequest);
            var attributes = oAuth2User.getAttributes();

            var providerId = String.valueOf(attributes.get("id"));
            var name = (String) attributes.get("login");
            var pictureUrl = (String) attributes.get("avatar_url");
            var email = (String) attributes.get("email");

            if (email == null) {
                email = fetchVerifiedEmail(userRequest.getAccessToken().getTokenValue());
            }
            if (email == null) {
                throw new OAuth2AuthenticationException(new OAuth2Error("github_no_verified_email", "No verified email found on GitHub account", null));
            }

            var info = new OAuthUserInfo(AuthProvider.GITHUB, providerId, email, true, name, pictureUrl);
            var user = provisioningService.findOrCreateUser(info);
            return new LocalOAuth2User(oAuth2User, user);
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("github_provisioning_error", "Failed to provision GitHub user", null), e);
        }
    }

    private String fetchVerifiedEmail(String accessToken) {
        List<Map<String, Object>> emails = restClient.get()
                .uri(GITHUB_EMAILS_URI)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (emails == null) {
            return null;
        }

        return emails.stream()
                .filter(e -> Boolean.TRUE.equals(e.get("verified")) && Boolean.TRUE.equals(e.get("primary")))
                .map(e -> (String) e.get("email"))
                .findFirst()
                .orElseGet(() -> emails.stream()
                        .filter(e -> Boolean.TRUE.equals(e.get("verified")))
                        .map(e -> (String) e.get("email"))
                        .findFirst()
                        .orElse(null));
    }
}