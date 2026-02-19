package com.marcos.leairning.security.jwt;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED_ERROR = new OAuth2Error(
            "invalid_token", "Token has been revoked", null);

    RevokedTokenService revokedTokenService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        var issuedAt = jwt.getIssuedAt();
        if (issuedAt != null && revokedTokenService.isRevoked(jwt.getSubject(), issuedAt)) {
            return OAuth2TokenValidatorResult.failure(REVOKED_ERROR);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
