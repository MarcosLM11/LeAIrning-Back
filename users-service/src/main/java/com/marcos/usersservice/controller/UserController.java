package com.marcos.usersservice.controller;

import com.marcos.usersservice.entity.dto.CreateUserDTO;
import com.marcos.usersservice.entity.dto.UpdateUserDTO;
import com.marcos.usersservice.entity.dto.UserDTO;
import com.marcos.usersservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RestController
@RequestMapping("/api/{version}/users")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    //GET users
    @GetMapping(version = "1.0")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info(">>> Request to get all users");
        var users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    //GET user
    @GetMapping(path = "/{userId}", version = "1.0")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        log.info(">>> Request to get User with id: {}", userId);
        var user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    //CREATE user
    @PostMapping(path = "/register" ,version = "1.0")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserDTO userDTO) {
        log.info(">>> Request to create User with username: {}", userDTO.username());
        userService.createUser(userDTO);
        return ResponseEntity.status(201).build();
    }

    //UPDATE user
    @PutMapping(path = "/{userId}", version = "1.0")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserDTO userDTO) {
        log.info(">>> Request to update User with id: {}", userId);
        var updatedUser = userService.updateUser(userId, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    //DELETE user
    @DeleteMapping(path = "/{userId}", version = "1.0")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info(">>> Request to delete User with id: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

}
