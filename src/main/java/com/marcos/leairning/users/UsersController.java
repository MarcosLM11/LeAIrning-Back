package com.marcos.leairning.users;

import com.marcos.leairning.security.annotations.BusinessAuthorityOnly;
import com.marcos.leairning.util.web.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.UUID;

@RestController
@BusinessAuthorityOnly
@RequiredArgsConstructor
@RequestMapping("/users")
public class UsersController {

    private final UsersService service;

    @GetMapping("/me")
    public UserResponseDTO getUser(@CurrentUserId UUID userId) {
        return service.get(userId);
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