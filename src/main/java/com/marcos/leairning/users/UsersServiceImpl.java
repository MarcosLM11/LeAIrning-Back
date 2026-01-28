package com.marcos.leairning.users;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class UsersServiceImpl implements UsersService {

    UsersRepository repository;

    @Override
    public UserResponseDTO get(UUID id) {

        val user = repository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Unable to find user with id: " + id )
        );

        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }

    public UserResponseDTO save(UserCreateDTO dto) {
        val user = new User();
        user.setEmail(dto.email());
        user.setPassword(dto.password());

        val savedUser = repository.save(user);

        return UserResponseDTO.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .build();
    }

    public UserResponseDTO update(UUID userId, UserUpdateDTO dto) {
        val user = repository.findById(userId).orElseThrow(
                        () -> new IllegalArgumentException("Unable to find user with id: " + userId )
        );

        user.setEmail(dto.email());
        user.setPassword(dto.password());

        repository.save(user);

        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {

            throw new IllegalArgumentException("Unable to find user with id: " + id);
        }

        repository.deleteById(id);
    }

}
