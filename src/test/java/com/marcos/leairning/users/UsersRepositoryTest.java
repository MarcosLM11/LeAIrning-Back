package com.marcos.leairning.users;

import com.marcos.leairning.AbstractRepositoryTest;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UsersRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    UsersRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByEmail_existingUser_returnsUser() {
        val user = createUser("find@test.com", "local");
        repository.save(user);
        val result = repository.findByEmail("find@test.com");
        assertTrue(result.isPresent());
        assertEquals("find@test.com", result.get().getEmail());
    }

    @Test
    void findByEmail_nonExisting_returnsEmpty() {
        val result = repository.findByEmail("nonexistent@test.com");
        assertTrue(result.isEmpty());
    }

    @Test
    void findByEmailAndProvider_matchingBoth_returnsUser() {
        val user = createUser("oauth@test.com", "google");
        repository.save(user);
        val result = repository.findByEmailAndProvider("oauth@test.com", "google");
        assertTrue(result.isPresent());
        assertEquals("google", result.get().getProvider());
    }

    @Test
    void findByEmailAndProvider_wrongProvider_returnsEmpty() {
        val user = createUser("oauth@test.com", "google");
        repository.save(user);
        val result = repository.findByEmailAndProvider("oauth@test.com", "github");
        assertTrue(result.isEmpty());
    }

    private User createUser(String email, String provider) {
        val user = new User();
        user.setEmail(email);
        user.setName("Test User");
        user.setRole("USER");
        user.setProvider(provider);
        user.setVerified(false);
        return user;
    }
}
