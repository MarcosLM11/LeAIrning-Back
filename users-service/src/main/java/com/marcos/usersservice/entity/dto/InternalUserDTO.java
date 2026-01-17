package com.marcos.usersservice.entity.dto;

public record InternalUserDTO(
        Long id,
        String username,
        String passwordHash,
        String role
) {
}