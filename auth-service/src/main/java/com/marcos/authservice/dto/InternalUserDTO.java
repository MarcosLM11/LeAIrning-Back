package com.marcos.authservice.dto;

public record InternalUserDTO(
        Long id,
        String username,
        String passwordHash,
        String role
) {
}
