package com.marcos.usersservice.service;

import com.marcos.usersservice.entity.dto.CreateUserDTO;
import com.marcos.usersservice.entity.dto.UpdateUserDTO;
import com.marcos.usersservice.entity.dto.UserDTO;
import java.util.List;

public interface UserService {

    List<UserDTO> getAllUsers();

    UserDTO getUserById(Long userId);

    UserDTO createUser(CreateUserDTO userDTO);

    UserDTO updateUser(Long userId, UpdateUserDTO userDTO);

    void deleteUser(Long userId);
}
