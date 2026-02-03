package com.marcos.leairning.security.auth;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthCodeResponse> login(@Valid @RequestBody LoginRequestDTO request) {
        val authCode = authService.login(request);
        return ResponseEntity.ok(new AuthCodeResponse(authCode));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.register(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<AuthCodeResponse> verify(@RequestParam String token) {
        val authCode = authService.verify(token);
        return ResponseEntity.ok(new AuthCodeResponse(authCode));
    }
}