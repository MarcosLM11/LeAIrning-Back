package com.marcos.usersservice.controller;

import com.marcos.usersservice.entity.dto.CreateUserDTO;
import com.marcos.usersservice.entity.dto.UpdateUserDTO;
import com.marcos.usersservice.entity.dto.UserDTO;
import com.marcos.usersservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/{version}/users")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    //GET users
    @GetMapping(version = "1.0")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        var users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    //GET user
    @GetMapping(path = "/{userId}", version = "1.0")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        var user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    //CREATE user
    @PostMapping(version = "1.0")
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserDTO userDTO) {
        var createdUser = userService.createUser(userDTO);
        return ResponseEntity.status(201).build();
    }

    //UPDATE user
    @PutMapping(path = "/{userId}", version = "1.0")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long userId, @RequestBody UpdateUserDTO userDTO) {
        var updatedUser = userService.updateUser(userId, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    //DELETE user
    @DeleteMapping(path = "/{userId}", version = "1.0")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

}
