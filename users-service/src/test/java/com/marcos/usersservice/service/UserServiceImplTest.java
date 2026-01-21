package com.marcos.usersservice.service;

import com.marcos.usersservice.entity.User;
import com.marcos.usersservice.entity.dto.CreateUserDTO;
import com.marcos.usersservice.entity.dto.UpdateUserDTO;
import com.marcos.usersservice.entity.dto.UserDTO;
import com.marcos.usersservice.event.NotificationEventPublisher;
import com.marcos.usersservice.reposiroty.UserRepository;
import com.marcos.usersservice.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationEventPublisher notificationPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDTO userDTO;
    private CreateUserDTO createUserDTO;
    private UpdateUserDTO updateUserDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        userDTO = new UserDTO(1L, "testuser", "test@example.com");
        createUserDTO = new CreateUserDTO("testuser", "password123", "test@example.com");
        updateUserDTO = new UpdateUserDTO("updateduser", "updated@example.com");
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // Given
        when(userMapper.toUser(createUserDTO)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userDTO);

        // When
        UserDTO result = userService.createUser(createUserDTO);

        // Then
        assertNotNull(result);
        assertEquals(userDTO.id(), result.id());
        assertEquals(userDTO.username(), result.username());
        assertEquals(userDTO.email(), result.email());

        verify(userMapper).toUser(createUserDTO);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("Should get user by id successfully")
    void shouldGetUserByIdSuccessfully() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userDTO);

        // When
        UserDTO result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userDTO.id(), result.id());
        assertEquals(userDTO.username(), result.username());
        assertEquals(userDTO.email(), result.email());

        verify(userRepository).findById(userId);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("Should throw exception when user not found by id")
    void shouldThrowExceptionWhenUserNotFoundById() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getUserById(userId));

        assertEquals("User not found with id: 1", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() {
        // Given
        Long userId = 1L;
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@example.com");

        UserDTO updatedUserDTO = new UserDTO(userId, "updateduser", "updated@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userMapper).updateUserFromDto(updateUserDTO, user);
        when(userRepository.save(user)).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedUserDTO);

        // When
        UserDTO result = userService.updateUser(userId, updateUserDTO);

        // Then
        assertNotNull(result);
        assertEquals(updatedUserDTO.id(), result.id());
        assertEquals(updatedUserDTO.username(), result.username());
        assertEquals(updatedUserDTO.email(), result.email());

        verify(userRepository).findById(userId);
        verify(userMapper).updateUserFromDto(updateUserDTO, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(updatedUser);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(userId, updateUserDTO));

        assertEquals("User not found with id: 1", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userMapper, never()).updateUserFromDto(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() {
        // Given
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        // Given
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(userId));

        assertEquals("User not found with id: 1", exception.getMessage());
        verify(userRepository).existsById(userId);
        verify(userRepository, never()).deleteById(userId);
    }
}