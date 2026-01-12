package com.marcos.usersservice.entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank @Size(min = 4, max = 20)
        String username,
        @NotBlank @Size(min = 6, max = 100)
        String password,
        @NotBlank @Email
        String email
) {
}
