package com.marcos.leairning.users;

import com.marcos.leairning.exception.UserNotFoundException;
import com.marcos.leairning.security.refreshtoken.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO get(UUID id) {
        return toResponse(findUserOrThrow(id));
    }

    public List<UserResponseDTO> getAll() {
        return repository.findAll().stream().map(UsersService::toResponse).toList();
    }

    public User getEntityById(UUID id) {
        return findUserOrThrow(id);
    }

    public User getEntityByEmail(String email) {
        return repository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponseDTO update(UUID userId, UserUpdateDTO dto) {
        var user = findUserOrThrow(userId);
        if (dto.email() != null) {
            user.setEmail(dto.email());
        }
        if (dto.password() != null) {
            user.setPassword(passwordEncoder.encode(dto.password()));
            refreshTokenRepository.revokeAllActiveByUserId(userId);
        }
        log.info("Updating user {}", userId);
        return toResponse(repository.save(user));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updatePassword(UUID userId, String encodedPassword) {
        var user = findUserOrThrow(userId);
        user.setPassword(encodedPassword);
        repository.save(user);
    }

    @Transactional
    public User updateVerifiedStatus(String email) {
        var user = getEntityByEmail(email);
        user.setVerified(true);
        log.info("User email verified: {}", email);
        return repository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        refreshTokenRepository.revokeAllActiveByUserId(id);
        repository.deleteById(id);
        log.info("Deleted user {}", id);
    }

    private User findUserOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private static UserResponseDTO toResponse(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .pictureUrl(user.getPictureUrl())
                .role(user.getRole())
                .verified(user.isVerified())
                .provider(user.getProvider())
                .build();
    }
}