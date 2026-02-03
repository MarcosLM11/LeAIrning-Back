package com.marcos.leairning.users;

import com.marcos.leairning.security.auth.RegisterRequestDTO;
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

    User toUser(RegisterRequestDTO dto);

    UserResponseDTO toResponse(User user);

    @Mapping(target = "email", expression = "java((String) oAuth2User.getAttribute(\"email\"))")
    @Mapping(target = "name", expression = "java((String) oAuth2User.getAttribute(\"name\"))")
    @Mapping(target = "pictureUrl", expression = "java((String) oAuth2User.getAttribute(\"picture\"))")
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "password", ignore = true)
    RegisterRequestDTO toCreateDTO(OAuth2User oAuth2User);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "lastUpdatedTimestamp", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUserFromDto(UserUpdateDTO dto, @MappingTarget User user);

}
