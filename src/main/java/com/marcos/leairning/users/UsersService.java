package com.marcos.leairning.users;

import java.util.UUID;

public interface UsersService {

    UserResponseDTO get(UUID id);

    UserResponseDTO save(UserCreateDTO user);

    UserResponseDTO update(UUID userId, UserUpdateDTO user);

    void delete(UUID id);

}
