package com.marcos.leairning.users;

import com.marcos.leairning.security.auth.RegisterRequestDTO;
import com.marcos.leairning.security.oauth2.Oauth2UserCreateDTO;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsersMapperTest {

    UsersMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UsersMapperImpl();
    }

    @Test
    void toUser_fromRegisterRequest_mapsFieldsAndSetsLocalProvider() {
        val dto = new RegisterRequestDTO("user@test.com", "Test User", "http://pic.url", "USER", "password12345");
        val user = mapper.toUser(dto);
        assertEquals("user@test.com", user.getEmail());
        assertEquals("Test User", user.getName());
        assertEquals("http://pic.url", user.getPictureUrl());
        assertEquals("USER", user.getRole());
        assertEquals("password12345", user.getPassword());
        assertEquals("local", user.getProvider());
    }

    @Test
    void toUser_fromRegisterRequest_withNull_returnsNull() {
        assertNull(mapper.toUser((RegisterRequestDTO) null));
    }

    @Test
    void toUser_fromOauth2DTO_mapsFields() {
        val dto = new Oauth2UserCreateDTO("oauth@test.com", "OAuth User", "http://pic.url", "google");
        val user = mapper.toUser(dto);
        assertEquals("oauth@test.com", user.getEmail());
        assertEquals("OAuth User", user.getName());
        assertEquals("http://pic.url", user.getPictureUrl());
        assertEquals("google", user.getProvider());
    }

    @Test
    void toUser_fromOauth2DTO_withNull_returnsNull() {
        assertNull(mapper.toUser((Oauth2UserCreateDTO) null));
    }

    @Test
    void toResponse_mapsAllFields() {
        val user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setName("Test");
        user.setPictureUrl("http://pic.url");
        user.setRole("USER");
        user.setVerified(true);
        user.setProvider("local");
        val response = mapper.toResponse(user);
        assertEquals(user.getId(), response.id());
        assertEquals("test@test.com", response.email());
        assertEquals("Test", response.name());
        assertEquals("http://pic.url", response.pictureUrl());
        assertEquals("USER", response.role());
        assertTrue(response.verified());
        assertEquals("local", response.provider());
    }

    @Test
    void toResponse_withNull_returnsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void toGoogleCreateDTO_mapsGoogleAttributes() {
        val oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("google@test.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Google User");
        when(oAuth2User.getAttribute("picture")).thenReturn("http://google.pic");
        val dto = mapper.toGoogleCreateDTO(oAuth2User);
        assertEquals("google@test.com", dto.email());
        assertEquals("Google User", dto.name());
        assertEquals("http://google.pic", dto.pictureUrl());
        assertEquals("google", dto.provider());
    }

    @Test
    void toGithubCreateDTO_mapsGithubAttributes() {
        val oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("github@test.com");
        when(oAuth2User.getAttribute("login")).thenReturn("githubuser");
        when(oAuth2User.getAttribute("avatar_url")).thenReturn("http://github.pic");
        val dto = mapper.toGithubCreateDTO(oAuth2User);
        assertEquals("github@test.com", dto.email());
        assertEquals("githubuser", dto.name());
        assertEquals("http://github.pic", dto.pictureUrl());
        assertEquals("github", dto.provider());
    }

    @Test
    void toOauth2CreateDTO_delegatesToGoogle() {
        val oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("g@test.com");
        when(oAuth2User.getAttribute("name")).thenReturn("G User");
        when(oAuth2User.getAttribute("picture")).thenReturn("http://g.pic");
        val dto = mapper.toOauth2CreateDTO(oAuth2User, "google");
        assertEquals("google", dto.provider());
    }

    @Test
    void toOauth2CreateDTO_delegatesToGithub() {
        val oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("gh@test.com");
        when(oAuth2User.getAttribute("login")).thenReturn("ghuser");
        when(oAuth2User.getAttribute("avatar_url")).thenReturn("http://gh.pic");
        val dto = mapper.toOauth2CreateDTO(oAuth2User, "github");
        assertEquals("github", dto.provider());
    }

    @Test
    void toOauth2CreateDTO_withUnknownProvider_throws() {
        val oAuth2User = mock(OAuth2User.class);
        assertThrows(IllegalArgumentException.class, () -> mapper.toOauth2CreateDTO(oAuth2User, "unknown"));
    }

    @Test
    void updateUserFromDto_updatesEmailOnly() {
        val user = new User();
        user.setEmail("old@test.com");
        user.setPassword("oldpassword");
        user.setName("Original");
        val dto = new UserUpdateDTO("new@test.com", null);
        mapper.updateUserFromDto(dto, user);
        assertEquals("new@test.com", user.getEmail());
        assertEquals("oldpassword", user.getPassword());
        assertEquals("Original", user.getName());
    }

    @Test
    void updateUserFromDto_withNullEmail_doesNotOverwrite() {
        val user = new User();
        user.setEmail("keep@test.com");
        val dto = new UserUpdateDTO(null, null);
        mapper.updateUserFromDto(dto, user);
        assertEquals("keep@test.com", user.getEmail());
    }

    @Test
    void updateUserFromDto_withNullDto_doesNothing() {
        val user = new User();
        user.setEmail("keep@test.com");
        mapper.updateUserFromDto(null, user);
        assertEquals("keep@test.com", user.getEmail());
    }
}
