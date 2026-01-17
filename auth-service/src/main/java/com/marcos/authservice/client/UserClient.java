package com.marcos.authservice.client;

import com.marcos.authservice.dto.InternalUserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Optional;

@Component
public class UserClient {

    private final WebClient webClient;

    public UserClient(@Value("${users-service.base-url}") String baseUrl, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Optional<InternalUserDTO> findByUsername(String username) {
        return webClient.get()
                .uri("/internal/users/by-username/{username}", username)
                .retrieve()
                .bodyToMono(InternalUserDTO.class)
                .onErrorReturn(null)
                .blockOptional();
    }
}
