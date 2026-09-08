package com.marcos.leairning.security.jwt;

import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED_ERROR = new OAuth2Error("invalid_token", "Token has been revoked", null);
    private final RevokedTokenService revokedTokenService;

    public RevokedTokenValidator(RevokedTokenService revokedTokenService) {
        this.revokedTokenService = revokedTokenService;
    }

    @Override
    public @NonNull OAuth2TokenValidatorResult validate(Jwt jwt) {
        var issuedAt = jwt.getIssuedAt();
        if (issuedAt != null && revokedTokenService.isRevoked(jwt.getSubject(), issuedAt)) {
            return OAuth2TokenValidatorResult.failure(REVOKED_ERROR);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
