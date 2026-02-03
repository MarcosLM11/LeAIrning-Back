package com.marcos.leairning.users;

import com.marcos.leairning.security.auth.RegisterRequestDTO;

import java.util.Optional;
import java.util.UUID;

public interface UsersService {

    UserResponseDTO get(UUID id);

    Optional<UserResponseDTO> getByEmail(String email);

    User getEntityByEmail(String email);

    UserResponseDTO save(RegisterRequestDTO user);

    UserResponseDTO update(UUID userId, UserUpdateDTO user);

    UserResponseDTO updateVerifiedStatus(String email);

    void delete(UUID id);
}