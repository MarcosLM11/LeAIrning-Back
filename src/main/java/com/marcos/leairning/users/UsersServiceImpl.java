package com.marcos.leairning.users;

import com.marcos.leairning.exception.EmailAlreadyRegisteredException;
import com.marcos.leairning.exception.UserNotFoundException;
import com.marcos.leairning.security.auth.RegisterRequestDTO;
import com.marcos.leairning.security.oauth2.Oauth2UserCreateDTO;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Flogger
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
        log.atFine().log("Fetching user with id: %s", id);
        return mapper.toResponse(findUserOrThrow(id));
    }

    @Override
    public Optional<UserResponseDTO> getByEmail(String email) {
        log.atFine().log("Fetching user by email: %s", email);
        return repository.findByEmail(email).map(mapper::toResponse);
    }

    @Override
    public User getEntityByEmail(String email) {
        log.atFine().log("Fetching user entity by email: %s", email);
        return findUserByEmailOrThrow(email);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserResponseDTO save(RegisterRequestDTO dto) {
        log.atInfo().log("Registering new user with email: %s", dto.email());
        validateEmailNotRegistered(dto.email());
        
        val user = mapper.toUser(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(DEFAULT_ROLE);
        user.setVerified(false);

        val savedUser = repository.save(user);
        log.atInfo().log("User registered successfully with id: %s", savedUser.getId());

        return mapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserResponseDTO saveOauth2User(Oauth2UserCreateDTO dto) {
        log.atInfo().log("Registering OAuth2 user with email: %s", dto.email());
        validateEmailNotRegistered(dto.email());
        
        val user = mapper.toUser(dto);
        user.setRole(DEFAULT_ROLE);
        user.setVerified(true);
        
        val savedUser = repository.save(user);
        log.atInfo().log("OAuth2 user registered successfully with id: %s", savedUser.getId());

        return mapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDTO update(UUID userId, UserUpdateDTO dto) {
        log.atInfo().log("Updating user with id: %s", userId);
        val user = findUserOrThrow(userId);
        user.setEmail(dto.email());
        user.setPassword(dto.password());
        
        repository.save(user);
        log.atInfo().log("User updated successfully: %s", userId);

        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateVerifiedStatus(String email) {
        log.atInfo().log("Updating verified status for email: %s", email);
        val user = findUserByEmailOrThrow(email);
        user.setVerified(true);
        
        repository.save(user);
        log.atInfo().log("User verified successfully: %s", email);

        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(UUID id) {
        log.atInfo().log("Deleting user with id: %s", id);
        
        if (!repository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        
        repository.deleteById(id);
        log.atInfo().log("User deleted successfully: %s", id);
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
}
