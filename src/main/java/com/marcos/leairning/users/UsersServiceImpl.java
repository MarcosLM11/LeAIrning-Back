package com.marcos.leairning.users;

import com.marcos.leairning.exception.EmailAlreadyRegisteredException;
import com.marcos.leairning.exception.UserNotFoundException;
import com.marcos.leairning.security.auth.RegisterRequestDTO;
import com.marcos.leairning.security.jwt.RevokedTokenService;
import com.marcos.leairning.security.oauth2.Oauth2UserCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UsersServiceImpl implements UsersService {

    private static final Logger log = LoggerFactory.getLogger(UsersServiceImpl.class);
    private static final String DEFAULT_ROLE = "USER";

    private final UsersRepository repository;
    private final UsersMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final RevokedTokenService revokedTokenService;

    public UsersServiceImpl(UsersRepository repository, UsersMapper mapper, PasswordEncoder passwordEncoder,  RevokedTokenService revokedTokenService) {
        this.repository = repository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.revokedTokenService = revokedTokenService;
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO get(UUID id) {
        log.info("Fetching user with id: {}", id);
        return mapper.toResponse(findUserOrThrow(id));
    }

    @Override
    public Optional<UserResponseDTO> getByEmail(String email) {
        log.info("Fetching user by email: {}", email);
        return repository.findByEmail(email).map(mapper::toResponse);
    }

    @Override
    public Optional<UserResponseDTO> getByEmailAndProvider(String email, String provider) {
        log.info("Fetching user by email: {} and provider: {}", email, provider);
        return repository.findByEmailAndProvider(email, provider).map(mapper::toResponse);
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
        var user = mapper.toUser(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(DEFAULT_ROLE);
        user.setVerified(false);
        var savedUser = repository.save(user);
        log.atInfo().log("User registered successfully with id: {}", savedUser.getId());
        return mapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserResponseDTO saveOauth2User(Oauth2UserCreateDTO dto) {
        log.atInfo().log("Registering OAuth2 user with email: {}, provider: {}", dto.email(), dto.provider());
        validateEmailNotRegisteredForProvider(dto.email(), dto.provider());
        var user = mapper.toUser(dto);
        user.setRole(DEFAULT_ROLE);
        user.setVerified(true);
        user.setProvider(dto.provider());
        var savedUser = repository.save(user);
        log.atInfo().log("OAuth2 user registered successfully with id: {}", savedUser.getId());
        return mapper.toResponse(savedUser);
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
        revokedTokenService.revokeAllForUser(userId);
        log.atInfo().log("User updated successfully: {}", userId);
        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateVerifiedStatus(String email) {
        log.atInfo().log("Updating verified status for email: {}", email);
        var user = findUserByEmailOrThrow(email);
        user.setVerified(true);
        repository.save(user);
        log.atInfo().log("User verified successfully: {}", email);
        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(UUID id) {
        log.atInfo().log("Deleting user with id: {}", id);
        if (!repository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        repository.deleteById(id);
        revokedTokenService.revokeAllForUser(id);
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

    private void validateEmailNotRegisteredForProvider(String email, String provider) {
        if (repository.findByEmailAndProvider(email, provider).isPresent()) {
            throw new EmailAlreadyRegisteredException(email + " (provider: " + provider + ")");
        }
    }
}
