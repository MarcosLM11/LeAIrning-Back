package com.marcos.leairning.security.jwt;

import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.users.UserResponseDTO;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static java.util.Collections.singletonList;

@Service
public class JwtService {

    public static final String SCOPE = "scope";
    public static final String ROLES = "roles";
    private static final String SELF = "self";
    private static final String BUSINESS_SCOPE = "business";
    private static final String REFRESH_TOKEN_SCOPE = "refresh-token";

    private final JwtEncoder encoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder encoder, JwtProperties jwtProperties) {
        this.encoder = encoder;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(UserResponseDTO user) {
        var ttl = jwtProperties.getAccessTokenTtl();
        return generateToken(user, ttl, BUSINESS_SCOPE);
    }

    public String generateRefreshToken(UserResponseDTO user) {
        var ttl = jwtProperties.getRefreshTokenTtl();
        return generateToken(user, ttl, REFRESH_TOKEN_SCOPE);
    }

    public TokenPair rotateFromJwt(Jwt jwt) {
        var subject = jwt.getSubject();
        var roles = jwt.getClaimAsStringList(ROLES);
        var accessToken = generateTokenFromClaims(subject, roles, jwtProperties.getAccessTokenTtl(), BUSINESS_SCOPE);
        var refreshToken = generateTokenFromClaims(subject, roles, jwtProperties.getRefreshTokenTtl(), REFRESH_TOKEN_SCOPE);
        return new TokenPair(accessToken, refreshToken);
    }

    private String generateTokenFromClaims(String subject, List<String> roles, Duration ttl, String scope) {
        var now = Instant.now();
        var expiryDate = now.plus(ttl);
        return getJwtClaims(subject, roles, scope, now, expiryDate);
    }

    private String generateToken(UserResponseDTO user, Duration ttl, String scope) {
        var now = Instant.now();
        var expiryDate = now.plus(ttl);
        var userId = user.id().toString();
        var role = user.role();
        var roles = singletonList(role);

        return getJwtClaims(userId, roles, scope, now, expiryDate);
    }

    private String getJwtClaims(String subject, List<String> roles, String scope, Instant now, Instant expiryDate) {
        var claims = JwtClaimsSet.builder()
                .claim(SCOPE, scope)
                .issuer(SELF)
                .issuedAt(now)
                .expiresAt(expiryDate)
                .subject(subject)
                .claim(ROLES, roles)
                .build();
        var jwsHeader = JwsHeader.with(MacAlgorithm.HS512).build();
        var parameters = JwtEncoderParameters.from(jwsHeader, claims);
        return encoder.encode(parameters).getTokenValue();
    }
}