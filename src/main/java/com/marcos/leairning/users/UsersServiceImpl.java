package com.marcos.leairning.users;

import com.marcos.leairning.exception.EmailAlreadyRegisteredException;
import com.marcos.leairning.exception.UserNotFoundException;
import com.marcos.leairning.security.auth.RegisterRequestDTO;
import com.marcos.leairning.security.refreshtoken.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {
    private static final UserRole DEFAULT_ROLE = UserRole.ROLE_USER;
    private final UsersRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO get(UUID id) {
        log.info("Fetching user with id: {}", id);
        return toResponse(findUserOrThrow(id));
    }

    @Override
    public User getEntityById(UUID id) {
        return findUserOrThrow(id);
    }

    @Override
    public Optional<UserResponseDTO> getByEmail(String email) {
        log.info("Fetching user by email: {}", email);
        return repository.findByEmail(email).map(UsersServiceImpl::toResponse);
    }

    @Override
    public Optional<User> findEntityByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public User getEntityByEmail(String email) {
        log.info("Fetching user entity by email: {}", email);
        return findUserByEmailOrThrow(email);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserResponseDTO save(RegisterRequestDTO dto) {
        log.atInfo().log("Registering new user with email: {}", dto.email());
        validateEmailNotRegistered(dto.email());
        var user = User.builder()
                .email(dto.email())
                .username(dto.name())
                .pictureUrl(dto.pictureUrl())
                .password(passwordEncoder.encode(dto.password()))
                .role(DEFAULT_ROLE)
                .verified(false)
                .provider("local")
                .build();
        var savedUser = repository.save(user);
        log.atInfo().log("User registered successfully with id: {}", savedUser.getId());
        return toResponse(savedUser);
    }

    @Override
    @Transactional
    public User createOAuthUser(String email, String username, String pictureUrl, String provider) {
        log.atInfo().log("Provisioning OAuth2 user with email: {}, provider: {}", email, provider);
        var user = User.builder()
                .email(email)
                .username(username)
                .pictureUrl(pictureUrl)
                .role(DEFAULT_ROLE)
                .verified(true)
                .provider(provider)
                .build();
        return repository.save(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponseDTO update(UUID userId, UserUpdateDTO dto) {
        log.atInfo().log("Updating user with id: {}", userId);
        var user = findUserOrThrow(userId);
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        repository.save(user);
        refreshTokenRepository.revokeAllActiveByUserId(userId);
        log.atInfo().log("User updated successfully: {}", userId);
        return toResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updatePassword(UUID userId, String encodedPassword) {
        var user = findUserOrThrow(userId);
        user.setPassword(encodedPassword);
        repository.save(user);
    }

    @Override
    @Transactional
    public User updateVerifiedStatus(String email) {
        log.atInfo().log("Updating verified status for email: {}", email);
        var user = findUserByEmailOrThrow(email);
        user.setVerified(true);
        repository.save(user);
        log.atInfo().log("User verified successfully: {}", email);
        return user;
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(UUID id) {
        log.atInfo().log("Deleting user with id: {}", id);
        if (!repository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        refreshTokenRepository.revokeAllActiveByUserId(id);
        repository.deleteById(id);
        log.atInfo().log("User deleted successfully: {}", id);
    }

    private User findUserOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private User findUserByEmailOrThrow(String email) {
        return repository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }

    private void validateEmailNotRegistered(String email) {
        if (repository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }
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