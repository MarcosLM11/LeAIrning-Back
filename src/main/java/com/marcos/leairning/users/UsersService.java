package com.marcos.leairning.users;

import com.marcos.leairning.security.auth.RegisterRequestDTO;
import java.util.Optional;
import java.util.UUID;

public interface UsersService {

    UserResponseDTO get(UUID id);
    User getEntityById(UUID id);
    Optional<UserResponseDTO> getByEmail(String email);
    Optional<User> findEntityByEmail(String email);
    User getEntityByEmail(String email);
    UserResponseDTO save(RegisterRequestDTO user);
    User createOAuthUser(String email, String username, String pictureUrl, String provider);
    UserResponseDTO update(UUID userId, UserUpdateDTO user);
    void updatePassword(UUID userId, String encodedPassword);
    User updateVerifiedStatus(String email);
    void delete(UUID id);
}