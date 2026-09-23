package com.marcos.leairning.security.jwt;

import com.marcos.leairning.users.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {
    public static final String ROLES = "roles";
    private static final String SELF = "self";

    private final JwtEncoder encoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder encoder, JwtProperties jwtProperties) {
        this.encoder = encoder;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(User user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(SELF)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.getAccessTokenTtl()))
                .subject(user.getId().toString())
                .claim(ROLES, List.of(user.getRole().name()))
                .build();
        var jwsHeader = JwsHeader.with(MacAlgorithm.HS512).build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}