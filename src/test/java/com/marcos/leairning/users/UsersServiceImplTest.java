package com.marcos.leairning.users;

import com.marcos.leairning.exception.EmailAlreadyRegisteredException;
import com.marcos.leairning.exception.UserNotFoundException;
import com.marcos.leairning.security.auth.RegisterRequestDTO;
import com.marcos.leairning.security.jwt.RevokedTokenService;
import com.marcos.leairning.security.oauth2.Oauth2UserCreateDTO;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsersServiceImplTest {

    UsersRepository repository;
    UsersMapper mapper;
    PasswordEncoder passwordEncoder;
    RevokedTokenService revokedTokenService;
    UsersServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(UsersRepository.class);
        mapper = mock(UsersMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        revokedTokenService = mock(RevokedTokenService.class);
        service = new UsersServiceImpl(repository, mapper, passwordEncoder, revokedTokenService);
    }

    @Test
    void get_existingUser_returnsDTO() {
        val userId = UUID.randomUUID();
        val user = new User();
        user.setId(userId);
        val dto = UserResponseDTO.builder().id(userId).build();
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(dto);
        val result = service.get(userId);
        assertEquals(userId, result.id());
    }

    @Test
    void get_nonExistingUser_throwsUserNotFound() {
        val userId = UUID.randomUUID();
        when(repository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.get(userId));
    }

    @Test
    void getByEmail_existingUser_returnsOptional() {
        val user = new User();
        val dto = UserResponseDTO.builder().email("test@test.com").build();
        when(repository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(dto);
        val result = service.getByEmail("test@test.com");
        assertTrue(result.isPresent());
    }

    @Test
    void getByEmail_nonExisting_returnsEmpty() {
        when(repository.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertTrue(service.getByEmail("none@test.com").isEmpty());
    }

    @Test
    void getByEmailAndProvider_existingUser_returnsOptional() {
        val user = new User();
        val dto = UserResponseDTO.builder().email("o@t.com").provider("google").build();
        when(repository.findByEmailAndProvider("o@t.com", "google")).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(dto);
        assertTrue(service.getByEmailAndProvider("o@t.com", "google").isPresent());
    }

    @Test
    void getEntityByEmail_existingUser_returnsEntity() {
        val user = new User();
        user.setEmail("e@t.com");
        when(repository.findByEmail("e@t.com")).thenReturn(Optional.of(user));
        assertEquals(user, service.getEntityByEmail("e@t.com"));
    }

    @Test
    void getEntityByEmail_nonExisting_throwsUserNotFound() {
        when(repository.findByEmail("none@t.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.getEntityByEmail("none@t.com"));
    }

    @Test
    void save_newUser_encodesPasswordAndSaves() {
        val dto = new RegisterRequestDTO("new@t.com", "Name", null, "USER", "password12345");
        val user = new User();
        val savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        val responseDTO = UserResponseDTO.builder().id(savedUser.getId()).build();
        when(repository.findByEmail("new@t.com")).thenReturn(Optional.empty());
        when(mapper.toUser(dto)).thenReturn(user);
        when(passwordEncoder.encode("password12345")).thenReturn("encoded");
        when(repository.save(user)).thenReturn(savedUser);
        when(mapper.toResponse(savedUser)).thenReturn(responseDTO);
        val result = service.save(dto);
        assertNotNull(result.id());
        assertEquals("encoded", user.getPassword());
        assertEquals("USER", user.getRole());
        assertFalse(user.isVerified());
    }

    @Test
    void save_duplicateEmail_throwsEmailAlreadyRegistered() {
        val dto = new RegisterRequestDTO("dup@t.com", "Name", null, "USER", "password12345");
        when(repository.findByEmail("dup@t.com")).thenReturn(Optional.of(new User()));
        assertThrows(EmailAlreadyRegisteredException.class, () -> service.save(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void saveOauth2User_newUser_savesWithProviderAndVerified() {
        val dto = new Oauth2UserCreateDTO("o@t.com", "OAuth", "http://pic", "google");
        val user = new User();
        val savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        val responseDTO = UserResponseDTO.builder().id(savedUser.getId()).build();
        when(repository.findByEmailAndProvider("o@t.com", "google")).thenReturn(Optional.empty());
        when(mapper.toUser(dto)).thenReturn(user);
        when(repository.save(user)).thenReturn(savedUser);
        when(mapper.toResponse(savedUser)).thenReturn(responseDTO);
        val result = service.saveOauth2User(dto);
        assertNotNull(result.id());
        assertTrue(user.isVerified());
        assertEquals("google", user.getProvider());
    }

    @Test
    void saveOauth2User_duplicateEmailAndProvider_throws() {
        val dto = new Oauth2UserCreateDTO("o@t.com", "OAuth", "http://pic", "google");
        when(repository.findByEmailAndProvider("o@t.com", "google")).thenReturn(Optional.of(new User()));
        assertThrows(EmailAlreadyRegisteredException.class, () -> service.saveOauth2User(dto));
    }

    @Test
    void update_existingUser_updatesAndRevokesTokens() {
        val userId = UUID.randomUUID();
        val user = new User();
        user.setId(userId);
        val dto = new UserUpdateDTO("new@t.com", "newpassword12");
        val responseDTO = UserResponseDTO.builder().id(userId).email("new@t.com").build();
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword12")).thenReturn("encoded");
        when(repository.save(user)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(responseDTO);
        val result = service.update(userId, dto);
        assertEquals("new@t.com", result.email());
        verify(revokedTokenService).revokeAllForUser(userId);
    }

    @Test
    void updateVerifiedStatus_existingUser_setsVerifiedTrue() {
        val user = new User();
        user.setEmail("v@t.com");
        user.setVerified(false);
        val responseDTO = UserResponseDTO.builder().verified(true).build();
        when(repository.findByEmail("v@t.com")).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(responseDTO);
        val result = service.updateVerifiedStatus("v@t.com");
        assertTrue(user.isVerified());
        assertTrue(result.verified());
    }

    @Test
    void delete_existingUser_deletesAndRevokesTokens() {
        val userId = UUID.randomUUID();
        when(repository.existsById(userId)).thenReturn(true);
        service.delete(userId);
        verify(repository).deleteById(userId);
        verify(revokedTokenService).revokeAllForUser(userId);
    }

    @Test
    void delete_nonExistingUser_throwsUserNotFound() {
        val userId = UUID.randomUUID();
        when(repository.existsById(userId)).thenReturn(false);
        assertThrows(UserNotFoundException.class, () -> service.delete(userId));
        verify(repository, never()).deleteById(any());
    }
}
