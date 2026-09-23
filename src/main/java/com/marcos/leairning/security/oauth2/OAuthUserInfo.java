package com.marcos.leairning.security.oauth2;

public record OAuthUserInfo(
        AuthProvider provider,
        String providerId,
        String email,
        boolean emailVerified,
        String name,
        String pictureUrl
) {}