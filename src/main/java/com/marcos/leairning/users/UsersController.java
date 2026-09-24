package com.marcos.leairning.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final UsersService service;

    public UsersController(UsersService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO createUser(@Valid @RequestBody UserRequestDTO dto) {
        return service.create(dto);
    }

    @GetMapping
    public List<UserResponseDTO> getAllUsers() {
        return service.getAll();
    }

    @GetMapping("/{userId}")
    public UserResponseDTO getUser(@PathVariable UUID userId) {
        return service.get(userId);
    }

    @PutMapping("/{userId}")
    public UserResponseDTO updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRequestDTO dto) {
        return service.update(userId, dto);
    }

    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable UUID userId) {
        service.delete(userId);
    }
}