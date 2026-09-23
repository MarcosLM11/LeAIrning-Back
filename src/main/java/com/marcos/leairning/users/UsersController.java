package com.marcos.leairning.users;

import com.marcos.leairning.util.web.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@PreAuthorize("hasRole('ROLE_USER')")
public class UsersController {

    private final UsersService service;

    @GetMapping("/me")
    public UserResponseDTO getUser(@CurrentUserId UUID userId) {
        return service.get(userId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<UserResponseDTO> getAllUsers() {
        return service.getAll();
    }

    @PutMapping("/me")
    public UserResponseDTO updateUser(@CurrentUserId UUID userId, @RequestBody UserUpdateDTO dto) {
        return service.update(userId, dto);
    }

    @DeleteMapping("/me")
    public void deleteUser(@CurrentUserId UUID userId) {
        service.delete(userId);
    }
}