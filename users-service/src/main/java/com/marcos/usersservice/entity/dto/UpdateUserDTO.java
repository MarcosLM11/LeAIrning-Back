package com.marcos.usersservice.entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserDTO (
        @Size(min = 3, max = 50)
        String username,

        @Email @Size(min=6,max=100)
        String email
) {
}
