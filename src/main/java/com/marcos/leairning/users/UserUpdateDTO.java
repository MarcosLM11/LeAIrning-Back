package com.marcos.leairning.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
        @Email String email,
        @Size(min = 12) String password
) {
}
