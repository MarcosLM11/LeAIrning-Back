package com.marcos.leairning.users;

import com.marcos.leairning.security.annotations.BusinessAuthorityOnly;
import com.marcos.leairning.util.web.CurrentUserId;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@BusinessAuthorityOnly
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class UsersController {

    UsersService service;

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
