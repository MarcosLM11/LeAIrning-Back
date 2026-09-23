package com.marcos.leairning.security.auth;

import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.util.web.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequestDTO request) {
        service.register(request);
    }

    @PostMapping("/verify")
    public AuthCodeResponse verify(@RequestParam String token) {
        return new AuthCodeResponse(service.verify(token));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(service.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        return ResponseEntity.ok(service.refresh(request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequestDTO request) {
        service.logout(request);
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequestDTO request, @CurrentUserId UUID userId) {
        service.changePassword(userId, request);
    }
}