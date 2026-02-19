package com.marcos.leairning.security.auth;

import java.util.UUID;

public interface AuthService {

    String login(LoginRequestDTO request);

    void register(RegisterRequestDTO request);

    String verify(String token);

    void logout(UUID userId);
}