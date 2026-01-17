package com.marcos.authservice.controller;

import com.marcos.authservice.dto.LoginRequest;
import com.marcos.authservice.dto.RefreshRequest;
import com.marcos.authservice.dto.TokenResponse;
import com.marcos.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/1.0/auth")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var response = authService.login(request.username(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }
}
