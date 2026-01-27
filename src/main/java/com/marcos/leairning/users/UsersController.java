package com.marcos.leairning.users;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class UsersController {

    UsersService service;

    @PostMapping
    public UserResponseDTO createUser(@RequestBody UserCreateDTO dto) {
        return service.save(dto);
    }

    @GetMapping("/{userId}")
    public UserResponseDTO getUser(@PathVariable UUID userId) {
        return service.get(userId);
    }

    @PutMapping("/{userId}")
    public UserResponseDTO updateUser(@PathVariable UUID userId, @RequestBody UserUpdateDTO dto) {
        return service.update(userId,dto);
    }

    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable UUID userId) {
        service.delete(userId);
    }

}
