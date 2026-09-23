package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.users.User;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class LocalOidcUser extends DefaultOidcUser implements LocalUserPrincipal {
    private final User user;

    public LocalOidcUser(OidcUser delegate, User user) {
        super(delegate.getAuthorities(), delegate.getIdToken(), delegate.getUserInfo());
        this.user = user;
    }

    @Override
    public User getUser() {
        return user;
    }
}