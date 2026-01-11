package com.marcos.usersservice.service;

import com.marcos.usersservice.entity.dto.CreateUserDTO;
import com.marcos.usersservice.entity.dto.UpdateUserDTO;
import com.marcos.usersservice.entity.dto.UserDTO;
import com.marcos.usersservice.reposiroty.UserRepository;
import com.marcos.usersservice.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import java.util.List;
import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    public List<UserDTO> getAllUsers() {
        var users = userRepository.findAll();
        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserDTO getUserById(Long userId) {
        var optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User not found");
        }
        return userMapper.toResponse(optionalUser.get());
    }

    @Override
    public UserDTO createUser(CreateUserDTO userDTO) {
        var entity = userMapper.toUser(userDTO);
        var savedUser = userRepository.save(entity);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserDTO updateUser(Long userId, UpdateUserDTO userDTO) {
        var optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User not found");
        }
        var entity = optionalUser.get();
        userMapper.updateUserFromDto(userDTO, entity);
        var updatedUser = userRepository.save(entity);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        var exists = userRepository.existsById(userId);
        if (!exists) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(userId);
    }
}
