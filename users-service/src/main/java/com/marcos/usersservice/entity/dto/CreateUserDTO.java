package com.marcos.usersservice.entity.dto;

public record CreateUserDTO(
        String username,
        String password,
        String email
) {
}
