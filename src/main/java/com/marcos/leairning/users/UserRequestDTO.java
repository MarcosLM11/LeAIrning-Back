package com.marcos.leairning.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
        @NotBlank @Email String email,
        @NotBlank String username,
        @NotBlank @Size(min = 12) String password
) {}