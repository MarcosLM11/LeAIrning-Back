package com.marcos.leairning.users;

import com.marcos.leairning.security.auth.RegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class UsersServiceImpl implements UsersService {

    private static final String DEFAULT_ROLE = "USER";

    UsersRepository repository;
    UsersMapper mapper;
    PasswordEncoder passwordEncoder;

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO get(UUID id) {
        val user = repository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Unable to find user with id: " + id)
        );
        return mapper.toResponse(user);
    }

    @Override
    public Optional<UserResponseDTO> getByEmail(String email) {
        val user = repository.findByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("Unable to find user with email: " + email )
        );

        return Optional.of(mapper.toResponse(user));
    }

    @Override
    public User getEntityByEmail(String email) {
        return repository.findByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("Unable to find user with email: " + email )
        );
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserResponseDTO save(RegisterRequestDTO dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        val user = mapper.toUser(dto);

        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(DEFAULT_ROLE);
        user.setVerified(false);

        val savedUser = repository.save(user);
        return mapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDTO update(UUID userId, UserUpdateDTO dto) {

        val user = repository.findById(userId).orElseThrow(
                        () -> new IllegalArgumentException("Unable to find user with id: " + userId )
        );

        user.setEmail(dto.email());
        user.setPassword(dto.password());

        repository.save(user);

        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateVerifiedStatus(String email) {

        val user = repository.findByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("Unable to find user with email: " + email )
        );

        user.setVerified(true);

        repository.save(user);

        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(UUID id) {
        if (!repository.existsById(id)) {

            throw new IllegalArgumentException("Unable to find user with id: " + id);
        }

        repository.deleteById(id);
    }

}
