package com.marcos.leairning.users;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsersControllerTest {

    UsersService service;
    UsersController controller;

    @BeforeEach
    void setUp() {
        service = mock(UsersService.class);
        controller = new UsersController(service);
    }

    @Test
    void getUser_returnsUserResponse() {
        val userId = UUID.randomUUID();
        val expected = UserResponseDTO.builder()
                .id(userId).email("test@example.com").name("Test").build();
        when(service.get(userId)).thenReturn(expected);
        val result = controller.getUser(userId);
        assertEquals(expected, result);
        verify(service).get(userId);
    }

    @Test
    void updateUser_returnsUpdatedUser() {
        val userId = UUID.randomUUID();
        val dto = new UserUpdateDTO("new@example.com", null);
        val expected = UserResponseDTO.builder()
                .id(userId).email("new@example.com").name("Test").build();
        when(service.update(userId, dto)).thenReturn(expected);
        val result = controller.updateUser(userId, dto);
        assertEquals(expected, result);
        verify(service).update(userId, dto);
    }

    @Test
    void deleteUser_callsServiceDelete() {
        val userId = UUID.randomUUID();
        controller.deleteUser(userId);
        verify(service).delete(userId);
    }
}
