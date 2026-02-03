package com.marcos.leairning.users;

import com.marcos.leairning.security.auth.RegisterRequestDTO;
import com.marcos.leairning.security.oauth2.Oauth2UserCreateDTO;

import java.util.Optional;
import java.util.UUID;

public interface UsersService {

    UserResponseDTO get(UUID id);

    Optional<UserResponseDTO> getByEmail(String email);

    User getEntityByEmail(String email);

    UserResponseDTO save(RegisterRequestDTO user);

    UserResponseDTO saveOauth2User(Oauth2UserCreateDTO user);

    UserResponseDTO update(UUID userId, UserUpdateDTO user);

    UserResponseDTO updateVerifiedStatus(String email);

    void delete(UUID id);
}