package com.marcos.leairning.security.refreshtoken;

import com.marcos.leairning.security.annotations.RefreshTokenAuthorityOnly;
import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPair;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshTokenAuthorityOnly
@RestController
@RequestMapping("/token")
public class RefreshTokenController {

    private final JwtService jwtService;

    public RefreshTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@AuthenticationPrincipal Jwt jwt) {
        var tokenPair = jwtService.rotateFromJwt(jwt);
        return ResponseEntity.ok(tokenPair);
    }
}
