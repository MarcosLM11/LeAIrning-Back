package com.marcos.leairning.users;

import com.marcos.leairning.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository repository;

    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        log.info("Creating user {}", dto.email());
        var user = User.builder()
                .email(dto.email())
                .username(dto.username())
                .password(dto.password())
                .build();
        return mapToDto(repository.save(user));
    }

    public UserResponseDTO get(UUID id) {
        var user = repository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        return mapToDto(user);
    }

    public List<UserResponseDTO> getAll() {
        return repository.findAll().stream().map(UsersService::mapToDto).toList();
    }

    @Transactional
    public UserResponseDTO update(UUID id, UserRequestDTO dto) {
        log.info("Updating user {}", id);
        var user = repository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        user.setEmail(dto.email());
        user.setUsername(dto.username());
        user.setPassword(dto.password());
        return mapToDto(repository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleted user {}", id);
        if (!repository.existsById(id)) throw new NotFoundException("User not found: " + id);
        repository.deleteById(id);
    }

    private static UserResponseDTO mapToDto(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .createdTimestamp(user.getCreatedTimestamp())
                .build();
    }
}