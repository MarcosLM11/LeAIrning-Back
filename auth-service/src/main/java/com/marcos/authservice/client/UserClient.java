package com.marcos.authservice.client;

import com.marcos.authservice.dto.InternalUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;

@FeignClient(name = "users-service")
public interface UserClient {
    @GetMapping("/internal/users/by-username/{username}")
    Optional<InternalUserDTO> findByUsername(@PathVariable String username);
}
