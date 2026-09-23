package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.users.User;

public interface LocalUserPrincipal {
    User getUser();
}