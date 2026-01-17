package com.marcos.usersservice.controller;

import com.marcos.usersservice.entity.dto.InternalUserDTO;
import com.marcos.usersservice.exception.UserNotFoundException;
import com.marcos.usersservice.reposiroty.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class InternalUserController {

    UserRepository userRepository;

    @GetMapping("/by-username/{username}")
    public ResponseEntity<InternalUserDTO> getUserByUsername(@PathVariable String username) {
        var user = userRepository.findByUsername(username).orElseThrow(
                () -> new UserNotFoundException("User not found with username: " + username));
        var dto = new InternalUserDTO(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole().name()
        );
        return ResponseEntity.ok(dto);
    }
}