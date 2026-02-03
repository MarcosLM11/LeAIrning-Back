package com.marcos.leairning.security.oauth2;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record Oauth2UserCreateDTO(
        @Email @NotBlank String email,
        @NotBlank String name,
        String pictureUrl
) {
}
