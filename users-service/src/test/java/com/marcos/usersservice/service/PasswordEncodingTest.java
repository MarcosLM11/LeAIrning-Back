package com.marcos.usersservice.service;

import com.marcos.usersservice.entity.Role;
import com.marcos.usersservice.entity.User;
import com.marcos.usersservice.entity.dto.CreateUserDTO;
import com.marcos.usersservice.entity.dto.UserDTO;
import com.marcos.usersservice.reposiroty.UserRepository;
import com.marcos.usersservice.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Password Encoding Tests")
class PasswordEncodingTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Should hash password when creating user")
    void shouldHashPasswordWhenCreatingUser() {
        // Given
        var rawPassword = "plainPassword123";
        var hashedPassword = "$2a$10$hashedPasswordValue";
        var createUserDTO = new CreateUserDTO("testuser", rawPassword, "test@example.com");
        var user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword(rawPassword);
        user.setEmail("test@example.com");
        user.setRole(Role.USER);
        var savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setPassword(hashedPassword);
        savedUser.setEmail("test@example.com");
        savedUser.setRole(Role.USER);
        var userDTO = new UserDTO(1L, "testuser", "test@example.com");
        when(userMapper.toUser(createUserDTO)).thenReturn(user);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(userDTO);

        // When
        userService.createUser(createUserDTO);

        // Then
        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(passwordEncoder).encode(rawPassword);
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(hashedPassword);
    }

    @Test
    @DisplayName("Should set default role USER when creating user")
    void shouldSetDefaultRoleUserWhenCreatingUser() {
        // Given
        var createUserDTO = new CreateUserDTO("testuser", "password123", "test@example.com");
        var user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("test@example.com");
        var savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setRole(Role.USER);
        var userDTO = new UserDTO(1L, "testuser", "test@example.com");
        when(userMapper.toUser(createUserDTO)).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(userDTO);

        // When
        userService.createUser(createUserDTO);

        // Then
        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
    }
}