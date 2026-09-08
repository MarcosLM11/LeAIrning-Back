package com.marcos.leairning.security.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public  AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthCodeResponse> login(@Valid @RequestBody LoginRequestDTO request) {
        var authCode = authService.login(request);
        return ResponseEntity.ok(new AuthCodeResponse(authCode));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.register(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<AuthCodeResponse> verify(@RequestParam String token) {
        var authCode = authService.verify(token);
        return ResponseEntity.ok(new AuthCodeResponse(authCode));
    }
}