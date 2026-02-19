package com.marcos.leairning.security.refreshtoken;

import com.marcos.leairning.security.annotations.RefreshTokenAuthorityOnly;
import com.marcos.leairning.security.jwt.JwtService;
import com.marcos.leairning.security.token.TokenPair;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshTokenAuthorityOnly
@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenController {

    JwtService jwtService;

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@AuthenticationPrincipal Jwt jwt) {
        val tokenPair = jwtService.rotateFromJwt(jwt);
        return ResponseEntity.ok(tokenPair);
    }
}
