package com.marcos.usersservice.controller;

import com.marcos.usersservice.entity.Role;
import com.marcos.usersservice.entity.User;
import com.marcos.usersservice.entity.dto.InternalUserDTO;
import com.marcos.usersservice.exception.UserNotFoundException;
import com.marcos.usersservice.reposiroty.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InternalUserController Unit Tests")
class InternalUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InternalUserController internalUserController;

    @Test
    @DisplayName("Should return user data when username exists")
    void shouldReturnUserDataWhenUsernameExists() {
        // Given
        var username = "john.doe";
        var user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("$2a$10$hashedPassword");
        user.setEmail("john@example.com");
        user.setRole(Role.USER);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        var response = internalUserController.getUserByUsername(username);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().username()).isEqualTo(username);
        assertThat(response.getBody().passwordHash()).isEqualTo("$2a$10$hashedPassword");
        assertThat(response.getBody().role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should throw exception when username not found")
    void shouldThrowExceptionWhenUsernameNotFound() {
        // Given
        var username = "unknown";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> internalUserController.getUserByUsername(username))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("username: unknown");
    }

    @Test
    @DisplayName("Should return ADMIN role for admin user")
    void shouldReturnAdminRoleForAdminUser() {
        // Given
        var username = "admin";
        var user = new User();
        user.setId(2L);
        user.setUsername(username);
        user.setPassword("$2a$10$adminHashedPassword");
        user.setEmail("admin@example.com");
        user.setRole(Role.ADMIN);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        var response = internalUserController.getUserByUsername(username);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().role()).isEqualTo("ADMIN");
    }
}