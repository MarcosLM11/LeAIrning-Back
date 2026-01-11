package com.marcos.usersservice.entity.dto;

public record UserDTO(
        Long id,
        String username,
        String email
) {
}
