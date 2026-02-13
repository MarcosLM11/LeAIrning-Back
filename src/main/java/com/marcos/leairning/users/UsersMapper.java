package com.marcos.leairning.users;

import com.marcos.leairning.security.auth.RegisterRequestDTO;
import com.marcos.leairning.security.oauth2.Oauth2UserCreateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.BeanMapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UsersMapper {

    @Mapping(target = "provider", constant = "local")
    User toUser(RegisterRequestDTO dto);

    User toUser(Oauth2UserCreateDTO dto);

    UserResponseDTO toResponse(User user);

    /**
     * Creates Oauth2UserCreateDTO based on the provider type (google or github).
     * This method delegates to the appropriate provider-specific mapping method.
     */
    default Oauth2UserCreateDTO toOauth2CreateDTO(OAuth2User oAuth2User, String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> toGoogleCreateDTO(oAuth2User);
            case "github" -> toGithubCreateDTO(oAuth2User);
            default -> throw new IllegalArgumentException("Unknown OAuth2 provider: " + provider);
        };
    }

    @Mapping(target = "email", expression = "java((String) oAuth2User.getAttribute(\"email\"))")
    @Mapping(target = "name", expression = "java((String) oAuth2User.getAttribute(\"name\"))")
    @Mapping(target = "pictureUrl", expression = "java((String) oAuth2User.getAttribute(\"picture\"))")
    @Mapping(target = "provider", constant = "google")
    Oauth2UserCreateDTO toGoogleCreateDTO(OAuth2User oAuth2User);

    @Mapping(target = "email", expression = "java((String) oAuth2User.getAttribute(\"email\"))")
    @Mapping(target = "name", expression = "java((String) oAuth2User.getAttribute(\"login\"))")
    @Mapping(target = "pictureUrl", expression = "java((String) oAuth2User.getAttribute(\"avatar_url\"))")
    @Mapping(target = "provider", constant = "github")
    Oauth2UserCreateDTO toGithubCreateDTO(OAuth2User oAuth2User);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "lastUpdatedTimestamp", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUserFromDto(UserUpdateDTO dto, @MappingTarget User user);

}
