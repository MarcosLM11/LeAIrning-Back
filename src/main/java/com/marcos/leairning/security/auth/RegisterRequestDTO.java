package com.marcos.leairning.security.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @Email @NotBlank String email,
        @NotBlank String name,
        String pictureUrl,
        @NotBlank String role,
        @NotBlank @Size(min = 12) String password
) {
}
