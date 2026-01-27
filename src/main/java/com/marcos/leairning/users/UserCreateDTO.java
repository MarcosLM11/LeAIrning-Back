package com.marcos.leairning.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(
        @Email @NotBlank
        String email,
        @NotBlank @Size(min = 12)
        String password
) {
}
