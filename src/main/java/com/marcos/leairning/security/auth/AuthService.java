package com.marcos.leairning.security.auth;

public interface AuthService {

    String login(LoginRequestDTO request);

    void register(RegisterRequestDTO request);

    String verify(String token);
}