package com.marcos.leairning.security.oauth2.google;

import com.marcos.leairning.security.oauth2.AuthProvider;
import com.marcos.leairning.security.oauth2.OAuthUserInfo;
import com.marcos.leairning.security.oauth2.OAuthUserProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final OAuthUserProvisioningService provisioningService;
    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            var oidcUser = delegate.loadUser(userRequest);
            var info = new OAuthUserInfo(
                    AuthProvider.GOOGLE,
                    oidcUser.getSubject(),
                    oidcUser.getEmail(),
                    Boolean.TRUE.equals(oidcUser.getEmailVerified()),
                    oidcUser.getFullName(),
                    oidcUser.getPicture());
            var user = provisioningService.findOrCreateUser(info);
            return new GoogleOidcUser(oidcUser, user);
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("google_provisioning_error", "Failed to provision Google user", null), e);
        }
    }
}