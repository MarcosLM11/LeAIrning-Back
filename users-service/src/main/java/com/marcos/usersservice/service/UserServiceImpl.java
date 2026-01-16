package com.marcos.usersservice.service;

import com.marcos.usersservice.entity.dto.CreateUserDTO;
import com.marcos.usersservice.entity.dto.UpdateUserDTO;
import com.marcos.usersservice.entity.dto.UserDTO;
import com.marcos.usersservice.exception.UserNotFoundException;
import com.marcos.usersservice.reposiroty.UserRepository;
import com.marcos.usersservice.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
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
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        log.info("User with id: {} found", userId);
        return userMapper.toResponse(optionalUser.get());
    }

    @Override
    public UserDTO createUser(CreateUserDTO userDTO) {
        var entity = userMapper.toUser(userDTO);
        var savedUser = userRepository.save(entity);
        log.info("User with id: {} and username: {} created", savedUser.getId(), savedUser.getUsername());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserDTO updateUser(Long userId, UpdateUserDTO userDTO) {
        var optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        var entity = optionalUser.get();
        userMapper.updateUserFromDto(userDTO, entity);
        var updatedUser = userRepository.save(entity);
        log.info("User with id: {} and username: {} updated", updatedUser.getId(), updatedUser.getUsername());
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        var exists = userRepository.existsById(userId);
        if (!exists) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }
}
